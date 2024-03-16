package cn.youyou.yyrpc.core.consumer;


import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.util.MethodUtils;
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
            return null;
        }

        // 远程调用
        RpcResponse rpcResponse = remoteCall(method, args);

        // 结果反解析
        return returnTypeParse(rpcResponse, method.getReturnType());
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

    private Object returnTypeParse(RpcResponse rpcResponse, Class<?> returnType) {
        if (rpcResponse.isStatus()) {
            // 反解析RpcResponse里的data数据到对应的数据类型
            Object data = rpcResponse.getData();
            System.out.println("method.getReturnType() = " + returnType + ", data value = " + data);
            return data;


        } else {
            Exception ex = rpcResponse.getEx();
            //ex.printStackTrace();
            throw new RuntimeException(ex);
        }


//        if (rpcResponse.isStatus()) {
//            Object data = rpcResponse.getData();
//            System.out.println("method.getReturnType() = " + returnType);
//            if (data instanceof JSONObject jsonResult) {
//                if (Map.class.isAssignableFrom(type)) {
//                    Map resultMap = new HashMap();
//                    Type genericReturnType = method.getGenericReturnType();
//                    System.out.println(genericReturnType);
//                    if (genericReturnType instanceof ParameterizedType parameterizedType) {
//                        Class<?> keyType = (Class<?>)parameterizedType.getActualTypeArguments()[0];
//                        Class<?> valueType = (Class<?>)parameterizedType.getActualTypeArguments()[1];
//                        System.out.println("keyType  : " + keyType);
//                        System.out.println("valueType: " + valueType);
//                        jsonResult.entrySet().stream().forEach(
//                                e -> {
//                                    Object key = TypeUtils.cast(e.getKey(), keyType);
//                                    Object value = TypeUtils.cast(e.getValue(), valueType);
//                                    resultMap.put(key, value);
//                                }
//                        );
//                    }
//                    return resultMap;
//                }
//                return jsonResult.toJavaObject(type);
//            } else if (data instanceof JSONArray jsonArray) {
//                Object[] array = jsonArray.toArray();
//                if (type.isArray()) {
//                    Class<?> componentType = type.getComponentType();
//                    Object resultArray = Array.newInstance(componentType, array.length);
//                    for (int i = 0; i < array.length; i++) {
//                        Array.set(resultArray, i, array[i]);
//                    }
//                    return resultArray;
//                } else if (List.class.isAssignableFrom(type)) {
//                    List<Object> resultList = new ArrayList<>(array.length);
//                    Type genericReturnType = method.getGenericReturnType();
//                    System.out.println(genericReturnType);
//                    if (genericReturnType instanceof ParameterizedType parameterizedType) {
//                        Type actualType = parameterizedType.getActualTypeArguments()[0];
//                        System.out.println(actualType);
//                        for (Object o : array) {
//                            resultList.add(TypeUtils.cast(o, (Class<?>) actualType));
//                        }
//                    } else {
//                        resultList.addAll(Arrays.asList(array));
//                    }
//                    return resultList;
//                } else {
//                    return null;
//                }
//            } else {
//                return TypeUtils.cast(data, type);
//            }
//        } else {
//            Exception ex = rpcResponse.getEx();
//            //ex.printStackTrace();
//            throw new RuntimeException(ex);
//        }
    }
}
