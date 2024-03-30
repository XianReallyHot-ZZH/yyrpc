package cn.youyou.yyrpc.core.provider;

import cn.youyou.yyrpc.core.RpcException;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.meta.ProviderMeta;
import cn.youyou.yyrpc.core.util.TypeUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * 负责服务端接受请求后的服务调用工作
 */
public class ProviderInvoker {

    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    public ProviderInvoker(ProviderBootstrap providerBootstrap) {
        this.skeleton = providerBootstrap.getSkeleton();
    }

    /**
     * 处理rpc请求，调用对应的服务后，负责返回rpc结果
     *
     * @param request
     * @return
     */
    public RpcResponse<?> invoke(RpcRequest request) {
        RpcResponse rpcResponse = new RpcResponse();
        List<ProviderMeta> providerMetas = skeleton.get(request.getService());
        try {
            ProviderMeta providerMeta = findProviderMeta(providerMetas, request.getMethodSign());
            Method method = providerMeta.getMethod();
            Object[] args = processArgs(request.getArgs(), method.getParameterTypes());
            Object result = method.invoke(providerMeta.getServiceImpl(), args);
            rpcResponse.setStatus(Boolean.TRUE);
            rpcResponse.setData(result);
            return rpcResponse;
        } catch (IllegalAccessException e) {
            rpcResponse.setEx(new RpcException(e.getMessage()));
        } catch (InvocationTargetException e) {
            rpcResponse.setEx(new RpcException(e.getTargetException().getMessage()));
        }

        return rpcResponse;
    }

    /**
     * 由于序列化有可能会丢失数据类型，所以要对入参进行数据类型的强制转换处理，不然反射会报入参类型不匹配的错
     *
     * @param args
     * @param parameterTypes
     * @return
     */
    private Object[] processArgs(Object[] args, Class<?>[] parameterTypes) {
        if (args == null || args.length < 1) {
            return args;
        }
        Object[] actuals = new Object[args.length];
        for (int i = 0; i < actuals.length; i++) {
            actuals[i] = TypeUtils.cast(args[i], parameterTypes[i]);
        }
        return actuals;
    }

    private ProviderMeta findProviderMeta(List<ProviderMeta> providerMetas, String methodSign) {
        Optional<ProviderMeta> optional = providerMetas.stream()
                .filter(providerMeta -> providerMeta.getMethodSign().equals(methodSign))
                .findFirst();
        return optional.orElse(null);
    }

}
