package cn.youyou.yyrpc.core.util;

public class TypeUtils {

    /**
     * 强制类型转换
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

        // 处理数据结构类型 数组

        // 处理数据结构类型 Map







        return null;
    }

}
