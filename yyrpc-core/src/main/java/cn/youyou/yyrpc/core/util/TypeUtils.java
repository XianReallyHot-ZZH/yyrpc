package cn.youyou.yyrpc.core.util;

import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;

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

        // 在Http请求的接收端接受到报文时，对象类型在接受的时候会变成hashMap，需要转成对象
        if (origin instanceof HashMap map) {
            JSONObject jsonObject = new JSONObject(map);
            return jsonObject.toJavaObject(type);
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
                Array.set(resultArray, i, Array.get(origin, i));
            }
            return resultArray;
        }

        if (origin instanceof JSONObject jsonObject) {
            return jsonObject.toJavaObject(type);
        }

        return null;
    }

}
