package cn.youyou.yyrpc.core.consumer;


import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.util.MethodUtils;
import cn.youyou.yyrpc.core.util.TypeUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 具体的代理逻辑
 * 具体的代理内容为：
 *  1、实现rpc远程调
 *      （1）拼装请求报文
 *      （2）进行远程请求
 *  2、获取结果进行反解析返回对应的类型
 */
public class YYConsumerInvocationHandler implements InvocationHandler {

    final static MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient.Builder()
        .connectionPool(new ConnectionPool(16, 60, TimeUnit.SECONDS))
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build();

    private Class<?> service;

    public YYConsumerInvocationHandler(Class<?> service) {
        this.service = service;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 方法过滤，类似toString方法
        if (MethodUtils.checkLocalMethod(method)) {
            System.out.println("过滤掉原生方法，methodName = " + method.getName());
            return null;
        }

        // 远程调用
        RpcResponse rpcResponse = remoteCall(method, args);

        // 结果反解析
        return returnTypeParse(rpcResponse, method);
    }

    private RpcResponse remoteCall(Method method, Object[] args) {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setService(service.getCanonicalName());
        rpcRequest.setMethodSign(MethodUtils.methodSign(method));
        rpcRequest.setArgs(args);
        return post(rpcRequest);
    }

    private RpcResponse post(RpcRequest rpcRequest) {
        String reqJson = JSON.toJSONString(rpcRequest);
        System.out.println(" ===> reqJson = " + reqJson);

        Request request = new Request.Builder()
                .url("http://localhost:8080/")
                .post(RequestBody.create(reqJson, JSON_TYPE))
                .build();
        try {
            String respJson = client.newCall(request).execute().body().string();
            System.out.println(" ===> respJson = " + respJson);
            RpcResponse rpcResponse = JSON.parseObject(respJson, RpcResponse.class);
            return rpcResponse;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: 这里面的反序列化肯定还是有问题的，后续再优化了
     * @param rpcResponse
     * @param method
     * @return
     */
    private Object returnTypeParse(RpcResponse rpcResponse, Method method) {
        if (rpcResponse.isStatus()) {
            Class<?> returnType = method.getReturnType();
            // 反解析RpcResponse里的data数据到对应的数据类型
            Object data = rpcResponse.getData();
            System.out.println("method.getReturnType() = " + returnType + ", data value = " + data);

            // 请求端发起请求，接受来自服务端的返回时，对象在通过http接受时会被序列化成JSONObject
            if (data instanceof JSONObject jsonObject) {
                if (Map.class.isAssignableFrom(returnType)) {
                    Map resultMap = new HashMap();
                    Type genericReturnType = method.getGenericReturnType();
                    if (genericReturnType instanceof ParameterizedType parameterizedType) {
                        Type keyType = parameterizedType.getActualTypeArguments()[0];
                        Type valueType = parameterizedType.getActualTypeArguments()[1];
                        jsonObject.forEach((key, value) -> {
                            Object keyObject = TypeUtils.cast(key, (Class<?>) keyType);
                            Object valueObject = TypeUtils.cast(value, (Class<?>) valueType);
                            resultMap.put(keyObject, valueObject);
                        });
                    }
                    return resultMap;
                }
                return jsonObject.toJavaObject(returnType);
            } else if (data instanceof JSONArray jsonArray) {
                Object[] array = jsonArray.toArray();
                if (returnType.isArray()) { // 请求端发起请求，接受来自服务端的返回时，数组类型会被系列化成jsonArray
                    Class<?> componentType = returnType.getComponentType();
                    Object resultArray = Array.newInstance(componentType, array.length);
                    for (int i = 0; i < array.length; i++) {
                        Array.set(resultArray, i, array[i]);
                    }
                    return resultArray;
                } else if (List.class.isAssignableFrom(returnType)) { // 请求端发起请求，接受来自服务端的返回时，List类型会变成jsonArray
                    List<Object> resultList = new ArrayList<>(array.length);
                    Type genericReturnType = method.getGenericReturnType();
                    System.out.println(genericReturnType);
                    if (genericReturnType instanceof ParameterizedType parameterizedType) {
                        Type actualType = parameterizedType.getActualTypeArguments()[0];
                        System.out.println(actualType);
                        for (Object o : array) {
                            resultList.add(TypeUtils.cast(o, (Class<?>) actualType));
                        }
                    } else {
                        resultList.addAll(Arrays.asList(array));
                    }
                    return resultList;
                }else {
                    return null;
                }
            } else {
                // 基础类型转换
                return TypeUtils.cast(data, returnType);
            }
        } else {
            Exception ex = rpcResponse.getEx();
            //ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
