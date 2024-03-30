package cn.youyou.yyrpc.core.consumer;


import cn.youyou.yyrpc.core.RpcException;
import cn.youyou.yyrpc.core.api.Filter;
import cn.youyou.yyrpc.core.api.RpcContext;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.consumer.http.OkHttpInvoker;
import cn.youyou.yyrpc.core.meta.InstanceMeta;
import cn.youyou.yyrpc.core.util.MethodUtils;
import cn.youyou.yyrpc.core.util.TypeUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.List;

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

    public YYConsumerInvocationHandler(Class<?> service, RpcContext rpcContext, List<InstanceMeta> providers) {
        this.service = service;
        this.rpcContext = rpcContext;
        this.providers = providers;
        this.httpInvoker = new OkHttpInvoker(Integer.parseInt(rpcContext.getParameters().getOrDefault("app.timeout", "1000")));
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

        int retries = Integer.parseInt(rpcContext.getParameters().getOrDefault("app.retries", "1"));

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

                List<InstanceMeta> instances = rpcContext.getRouter().route(providers);
                InstanceMeta instance = rpcContext.getLoadBalancer().choose(instances);
                log.debug("loadBalancer.choose(urls) ==> " + instance);

                RpcResponse<?> rpcResponse = httpInvoker.post(rpcRequest, instance.toUrl());
                Object result = castReturnResult(method, rpcResponse);
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

    private Object castReturnResult(Method method, RpcResponse rpcResponse) {
        if (rpcResponse.isStatus()) {
            return TypeUtils.castMethodResult(method, rpcResponse.getData());
        } else {
            Exception exception = rpcResponse.getEx();
            if (exception instanceof RpcException ex) {
                throw ex;
            } else {
                throw new RpcException(exception, RpcException.UnknownEx);
            }

        }
    }
}
