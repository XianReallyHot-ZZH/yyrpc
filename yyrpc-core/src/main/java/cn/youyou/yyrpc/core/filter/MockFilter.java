package cn.youyou.yyrpc.core.filter;

import cn.youyou.yyrpc.core.api.Filter;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.util.MethodUtils;
import cn.youyou.yyrpc.core.util.MockUtils;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MockFilter implements Filter {
    @SneakyThrows
    @Override
    public Object preFilter(RpcRequest request) {
        Class<?> clazz = Class.forName(request.getService());
        Method method = findMethod(clazz, request.getMethodSign());
        return MockUtils.mock(method.getReturnType());
    }

    private Method findMethod(Class<?> clazz, String methodSign) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> !MethodUtils.checkLocalMethod(method))
                .filter(method -> methodSign.equals(MethodUtils.methodSign(method)))
                .findFirst().orElse(null);
    }

    @Override
    public Object postFilter(RpcRequest request, RpcResponse response, Object result) {
        return null;
    }


}
