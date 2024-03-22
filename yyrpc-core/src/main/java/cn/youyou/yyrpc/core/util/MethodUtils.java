package cn.youyou.yyrpc.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodUtils {

    /**
     * 是Object原生方法，没有被重写
     *
     * @param method
     * @return
     */
    public static boolean checkLocalMethod(final Method method) {
        return method.getDeclaringClass().equals(Object.class);
    }

    /**
     * 生成方法签名字符串
     *
     * @param method
     * @return
     */
    public static String methodSign(Method method) {
        StringBuilder sb = new StringBuilder(method.getName());
        sb.append("@").append(method.getParameterCount());
        Arrays.stream(method.getParameterTypes()).forEach(pt -> {
            sb.append("_").append(pt.getCanonicalName());
        });
        return sb.toString();
    }

    public static List<Field> findAnnotatedField(Class<?> aClass, Class<? extends Annotation> annotationClass) {
        ArrayList<Field> result = new ArrayList<>();
        while (aClass != null) {
            for (Field field : aClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotationClass)) {
                    result.add(field);
                }
            }
            aClass = aClass.getSuperclass();
        }

        return result;
    }

}
