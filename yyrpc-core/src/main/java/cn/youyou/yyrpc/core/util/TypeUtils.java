package cn.youyou.yyrpc.core.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
public class TypeUtils {

    /**
     * 强制类型转换
     * TODO：这里面强制类型转换有问题存在功能上的问题，后续碰到再优化
     * @param origin
     * @param type
     * @return
     */
    public static Object cast(Object origin, Class<?> type) {
        if (origin == null) {
            return null;
        }
        // 目标类是原始类的父类，那么相当于就不用转了
        if (type.isAssignableFrom(origin.getClass())) {
            return origin;
        }

        // 请求端发送数组类型的参数，在服务接收端会以List接受，所以需要将Lsit转成数组
        if (type.isArray()) {
            if (origin instanceof List<?> list) {
                origin = list.toArray();
            }
            int length = Array.getLength(origin);
            Class<?> componentType = type.getComponentType();
            Object resultArray = Array.newInstance(componentType, length);
            for (int i = 0; i < length; i++) {
                if (componentType.isPrimitive() || componentType.getPackageName().startsWith("java")) {
                    Array.set(resultArray, i, Array.get(origin, i));
                } else {
                    Object castObject = cast(Array.get(origin, i), componentType);
                    Array.set(resultArray, i, castObject);
                }
            }
            return resultArray;
        }

        // 在Http请求的接收端接受到报文时，对象类型在接受的时候会变成hashMap，需要转成对象
        if (origin instanceof HashMap map) {
            JSONObject jsonObject = new JSONObject(map);
            return jsonObject.toJavaObject(type);
        }


        if (origin instanceof JSONObject jsonObject) {
            return jsonObject.toJavaObject(type);
        }

        // 处理基础数据类型
        if(type.equals(Integer.class) || type.equals(Integer.TYPE)) {
            return Integer.valueOf(origin.toString());
        } else if(type.equals(Long.class) || type.equals(Long.TYPE)) {
            return Long.valueOf(origin.toString());
        } else if(type.equals(Float.class) || type.equals(Float.TYPE)) {
            return Float.valueOf(origin.toString());
        } else if(type.equals(Double.class) || type.equals(Double.TYPE)) {
            return Double.valueOf(origin.toString());
        } else if (type.equals(Byte.class) || type.equals(Byte.TYPE)) {
            return Byte.valueOf(origin.toString());
        } else if(type.equals(Short.class) || type.equals(Short.TYPE)) {
            return Short.valueOf(origin.toString());
        } else if(type.equals(Character.class) || type.equals(Character.TYPE)) {
            return Character.valueOf(origin.toString().charAt(0));
        } else if(type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
            return Boolean.valueOf(origin.toString());
        }

        return null;
    }

    public static Object castMethodResult(Method method, Object data) {
        Class<?> type = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();
        return castGeneric(data, type, genericReturnType);
    }

    public static Object castGeneric(Object data, Class<?> type, Type genericReturnType) {
        // 反解析RpcResponse里的data数据到对应的数据类型
        log.debug("method.getReturnType() = " + type);
        log.debug("method.getGenericReturnType() = " + genericReturnType);

        // 请求端发起请求，接受来自服务端的返回时，对象在通过http接受时会被序列化成JSONObject
        if (data instanceof JSONObject jsonObject) {
            if (Map.class.isAssignableFrom(type)) {
                Map resultMap = new HashMap();
                log.debug(genericReturnType.toString());
                if (genericReturnType instanceof ParameterizedType parameterizedType) {
                    Type keyType = parameterizedType.getActualTypeArguments()[0];
                    Type valueType = parameterizedType.getActualTypeArguments()[1];
                    log.debug("keyType  : " + keyType);
                    log.debug("valueType: " + valueType);
                    jsonObject.forEach((key, value) -> {
                        Object keyObject = cast(key, (Class<?>) keyType);
                        Object valueObject = cast(value, (Class<?>) valueType);
                        resultMap.put(keyObject, valueObject);
                    });
                }
                return resultMap;
            }
            return jsonObject.toJavaObject(type);
        } else if (data instanceof Map map) {
            if (Map.class.isAssignableFrom(type) && genericReturnType != null) {
                Map resultMap = new HashMap();
                log.debug(genericReturnType.toString());
                if (genericReturnType instanceof ParameterizedType parameterizedType) {
                    Type keyType = parameterizedType.getActualTypeArguments()[0];
                    Type valueType = parameterizedType.getActualTypeArguments()[1];
                    log.debug("keyType  : " + keyType);
                    log.debug("valueType: " + valueType);
                    map.forEach((key, value) -> {
                        Object keyObject = cast(key, (Class<?>) keyType);
                        Object valueObject = cast(value, (Class<?>) valueType);
                        resultMap.put(keyObject, valueObject);
                    });
                }
                return resultMap;
            } else {
                return cast(data, type);
            }
        } else if (data instanceof List list) {
            Object[] array = list.toArray();
            if (type.isArray()) { // 请求端发起请求，接受来自服务端的返回时，数组类型会被系列化成jsonArray
                Class<?> componentType = type.getComponentType();
                Object resultArray = Array.newInstance(componentType, array.length);
                for (int i = 0; i < array.length; i++) {
                    if (componentType.isPrimitive() || componentType.getPackageName().startsWith("java")) {
                        Array.set(resultArray, i, array[i]);
                    } else {
                        Object castObject = TypeUtils.cast(array[i], componentType);
                        Array.set(resultArray, i, castObject);
                    }
                }
                return resultArray;
            } else if (List.class.isAssignableFrom(type)) { // 请求端发起请求，接受来自服务端的返回时，List类型会变成jsonArray
                List<Object> resultList = new ArrayList<>(array.length);
                log.debug(genericReturnType.toString());
                if (genericReturnType instanceof ParameterizedType parameterizedType) {
                    Type actualType = parameterizedType.getActualTypeArguments()[0];
                    log.debug(actualType.toString());
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
            return cast(data, type);
        }
    }

}
