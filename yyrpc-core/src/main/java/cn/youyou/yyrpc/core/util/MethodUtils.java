package cn.youyou.yyrpc.core.util;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MethodUtils {

    /**
     * 是Object原生方法，没有被重写
     * @param method
     * @return
     */
    public static boolean checkLocalMethod(final Method method) {
        return method.getDeclaringClass().equals(Object.class);
    }

    /**
     * 生成方法签名字符串
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

}
