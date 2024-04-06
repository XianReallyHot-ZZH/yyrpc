package cn.youyou.yyrpc.core.consumer;


import cn.youyou.yyrpc.core.RpcException;
import cn.youyou.yyrpc.core.api.Filter;
import cn.youyou.yyrpc.core.api.RpcContext;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.consumer.http.OkHttpInvoker;
import cn.youyou.yyrpc.core.governance.SlidingTimeWindow;
import cn.youyou.yyrpc.core.meta.InstanceMeta;
import cn.youyou.yyrpc.core.util.MethodUtils;
import cn.youyou.yyrpc.core.util.TypeUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 具体的代理逻辑
 * 具体的代理内容为：
 * 1、实现rpc远程调用
 * （1）拼装请求报文
 * （2）进行远程请求
 * 2、获取结果进行反解析返回对应的类型
 */
@Slf4j
public class YYConsumerInvocationHandler implements InvocationHandler {

    private Class<?> service;

    private RpcContext rpcContext;

    private List<InstanceMeta> providers;

    private HttpInvoker httpInvoker;

    // 触发半开状态的定时任务线程池
    final ScheduledExecutorService executor;

    // key对应实例，这里简化为实例的url，value为这个实例对应的滑窗
    final Map<String, SlidingTimeWindow> windows = new HashMap<>();

    // 当前被隔离的坏点（实例）
    final List<InstanceMeta> isolatedProviders = new ArrayList<>();

    // 当前等待被半开试错的实例
    final List<InstanceMeta> halfOpenProviders = new ArrayList<>();


    public YYConsumerInvocationHandler(Class<?> service, RpcContext rpcContext, List<InstanceMeta> providers) {
        this.service = service;
        this.rpcContext = rpcContext;
        this.providers = providers;
        this.httpInvoker = new OkHttpInvoker(Integer.parseInt(rpcContext.getParameters().getOrDefault("consumer.timeout", "1000")));
        this.executor = Executors.newScheduledThreadPool(1);
        int halfOpenInitialDelay = Integer.parseInt(rpcContext.getParameters().getOrDefault("consumer.halfOpenInitialDelay", "10000"));
        int halfOpenDelay = Integer.parseInt(rpcContext.getParameters().getOrDefault("consumer.halfOpenDelay", "60000"));
        this.executor.scheduleWithFixedDelay(this::halfOpen, halfOpenInitialDelay, halfOpenDelay, TimeUnit.MILLISECONDS);
    }

    // 触发半开状态，将相应被隔离的实例，加入半开实例队列
    private void halfOpen() {
        log.debug(" ====> half open isolatedProviders: " + isolatedProviders);
        // 清理掉上次的，以这次被隔离的实例为准
        halfOpenProviders.clear();
        halfOpenProviders.addAll(isolatedProviders);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 方法过滤，类似toString方法
        if (MethodUtils.checkLocalMethod(method)) {
            log.debug("过滤掉原生方法，methodName = " + method.getName());
            return null;
        }

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setService(service.getCanonicalName());
        rpcRequest.setMethodSign(MethodUtils.methodSign(method));
        rpcRequest.setArgs(args);

        int retries = Integer.parseInt(rpcContext.getParameters().getOrDefault("consumer.retries", "1"));
        int faultLimit = Integer.parseInt(rpcContext.getParameters().getOrDefault("consumer.faultLimit", "10"));

        while (retries-- > 0) {
            log.debug(" ===> reties: " + retries);
            try {
                // 嵌入过滤逻辑(前置过滤)
                for (Filter filter : rpcContext.getFilters()) {
                    Object preResult = filter.preFilter(rpcRequest);
                    if (preResult != null) {
                        log.debug(filter.getClass().getName() + " ===> preFilter result: " + preResult);
                        return preResult;
                    }
                }

                InstanceMeta instance;
                // 半开检测
                synchronized (halfOpenProviders) {
                    if (halfOpenProviders.isEmpty()) {
                        List<InstanceMeta> instances = rpcContext.getRouter().route(providers);
                        instance = rpcContext.getLoadBalancer().choose(instances);
                        log.debug("loadBalancer.choose(urls) ==> " + instance);
                    } else {
                        // 直接remove保证只能重试一次
                        instance = halfOpenProviders.remove(0);
                        System.out.println("半开熔断尝试，尝试实例信息： " + instance);
                    }
                }

                // 异常检测与记录
                Object result;
                RpcResponse<?> rpcResponse;
                String url = instance.toUrl();
                try {
                    rpcResponse = httpInvoker.post(rpcRequest, url);
                    result = castReturnResult(method, rpcResponse);
                } catch (Exception e) {
                    synchronized (windows) {
                        SlidingTimeWindow window = windows.computeIfAbsent(url, key -> new SlidingTimeWindow());
                        window.record(System.currentTimeMillis());
                        log.debug("instance {} in window with {}", url, window.getSum());
                        if (window.getSum() > faultLimit) {
                            isolate(instance);
                        }
                    }
                    // 影响本来的报错，不要影响本来的报错重试机制
                    throw e;
                }

                // 半开检测后，如果访问正常了，那么将半开的实例，全开，放置可选实例池
                synchronized (providers) {
                    if (!providers.contains(instance)) {
                        isolatedProviders.remove(instance);
                        providers.add(instance);
                        log.debug("instance {} is recovered, isolatedProviders={}, providers={}", instance, isolatedProviders, providers);
                    }
                }

                // 嵌入过滤逻辑(后置过滤)
                for (Filter filter : rpcContext.getFilters()) {
                    Object postResult = filter.postFilter(rpcRequest, rpcResponse, result);
                    if (postResult != null) {
                        log.debug(filter.getClass().getName() + " ===> postFilter result: " + postResult);
                        return postResult;
                    }
                }
                return result;
            } catch (Exception ex) {
                // 超时进行重试，不是超时，那么直接异常
                if (!(ex.getCause() instanceof SocketTimeoutException)) {
                    throw new RpcException(ex);
                }
            }
        }
        return null;
    }

    private void isolate(InstanceMeta instance) {
        log.debug(" ==> isolate instance: " + instance);
        // 从正常候选者中移除
        providers.remove(instance);
        log.debug(" 移除要隔离的实例后，当前可选实例池，providers = {}", providers);
        // 添加进隔离的队列中
        if (!isolatedProviders.contains(instance)) {
            isolatedProviders.add(instance);
        }
        log.debug(" ==> isolatedProviders = {}", isolatedProviders);
    }

    private Object castReturnResult(Method method, RpcResponse rpcResponse) {
        if (rpcResponse.isStatus()) {
            return TypeUtils.castMethodResult(method, rpcResponse.getData());
        } else {
            RpcException exception = rpcResponse.getEx();
            if (exception != null) {
                log.error("response error.", exception);
                throw exception;
            }
            return null;
        }
    }
}
