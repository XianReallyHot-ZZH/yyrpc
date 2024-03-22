package cn.youyou.yyrpc.core.consumer;


import cn.youyou.yyrpc.core.api.RpcContext;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.consumer.http.OkHttpInvoker;
import cn.youyou.yyrpc.core.util.MethodUtils;
import cn.youyou.yyrpc.core.util.TypeUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 具体的代理逻辑
 * 具体的代理内容为：
 * 1、实现rpc远程调用
 * （1）拼装请求报文
 * （2）进行远程请求
 * 2、获取结果进行反解析返回对应的类型
 */
public class YYConsumerInvocationHandler implements InvocationHandler {

    private Class<?> service;

    private RpcContext rpcContext;

    private List<String> providers;

    private HttpInvoker httpInvoker = new OkHttpInvoker();

    public YYConsumerInvocationHandler(Class<?> service, RpcContext rpcContext, List<String> providers) {
        this.service = service;
        this.rpcContext = rpcContext;
        this.providers = providers;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 方法过滤，类似toString方法
        if (MethodUtils.checkLocalMethod(method)) {
            System.out.println("过滤掉原生方法，methodName = " + method.getName());
            return null;
        }

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setService(service.getCanonicalName());
        rpcRequest.setMethodSign(MethodUtils.methodSign(method));
        rpcRequest.setArgs(args);

        String url = (String) rpcContext.getLoadBalancer().choose(rpcContext.getRouter().route(providers));
        System.out.println("loadBalancer.choose(urls) ==> " + url);

        RpcResponse<?> rpcResponse = httpInvoker.post(rpcRequest, url);

        if (rpcResponse.isStatus()) {
            return TypeUtils.castMethodResult(method, rpcResponse.getData());
        } else {
            Exception ex = rpcResponse.getEx();
            throw new RuntimeException(ex);
        }
    }
}
