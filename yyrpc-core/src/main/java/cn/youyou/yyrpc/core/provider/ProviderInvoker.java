package cn.youyou.yyrpc.core.provider;

import cn.youyou.yyrpc.core.RpcException;
import cn.youyou.yyrpc.core.api.RpcContext;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.config.ProviderConfigProperties;
import cn.youyou.yyrpc.core.governance.SlidingTimeWindow;
import cn.youyou.yyrpc.core.meta.ProviderMeta;
import cn.youyou.yyrpc.core.util.TypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cn.youyou.yyrpc.core.RpcException.ExceedLimitEx;

/**
 * 负责服务端接受请求后的服务调用工作
 */
@Slf4j
public class ProviderInvoker {

    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    // provider的技术参数
    private Map<String, String> metas;

    private final int trafficControl;

    private Map<String, SlidingTimeWindow> windows = new HashMap<>();

    public ProviderInvoker(ProviderBootstrap providerBootstrap) {
        this.skeleton = providerBootstrap.getSkeleton();
        this.metas = providerBootstrap.getProviderProperties().getMetas();
        this.trafficControl = Integer.parseInt(metas.getOrDefault("tc", "0"));
    }

    /**
     * 处理rpc请求，调用对应的服务后，负责返回rpc结果
     *
     * @param request
     * @return
     */
    public RpcResponse<Object> invoke(RpcRequest request) {
        log.debug(" ===> ProviderInvoker.invoke(request:{})", request);
        // 获取跨节点参数
        if (!request.getParams().isEmpty()) {
            request.getParams().forEach(RpcContext::setContextParameters);
        }
        RpcResponse<Object> rpcResponse = new RpcResponse<>();

        // 流控检查
        if (trafficControl > 0) {
            String service = request.getService();
            synchronized (windows) {
                SlidingTimeWindow window = windows.computeIfAbsent(service, k -> new SlidingTimeWindow());
                int trafficCount = window.calcSum();
                if (trafficCount >= trafficControl) {
                    log.debug(" >>> 触发流控，window：" + window);
                    throw new RpcException("service " + service + " invoked in 30s/[" +
                            trafficCount + "] larger than tpsLimit = " + trafficControl, ExceedLimitEx);
                } else {
                    window.record(System.currentTimeMillis());
                    log.debug("service {} in window with {}", service, window.getSum());
                }
            }
        }

        List<ProviderMeta> providerMetas = skeleton.get(request.getService());
        try {
            ProviderMeta providerMeta = findProviderMeta(providerMetas, request.getMethodSign());
            Method method = providerMeta.getMethod();
            Object[] args = processArgs(request.getArgs(), method.getParameterTypes(), method.getGenericParameterTypes());
            Object result = method.invoke(providerMeta.getServiceImpl(), args);
            rpcResponse.setStatus(Boolean.TRUE);
            rpcResponse.setData(result);
            return rpcResponse;
        } catch (IllegalAccessException | IllegalArgumentException e) {
//            e.printStackTrace();
            rpcResponse.setEx(new RpcException(e.getMessage()));
        } catch (InvocationTargetException e) {
//            e.printStackTrace();
            rpcResponse.setEx(new RpcException(e.getTargetException().getMessage()));
        } finally {
            // 释放跨节点参数，因为这个是可线程绑定的，要释放
            RpcContext.ContextParameters.get().clear();
        }
        log.debug(" ===> ProviderInvoker.invoke() = {}", rpcResponse);
        return rpcResponse;
    }

    /**
     * 由于序列化有可能会丢失数据类型，所以要对入参进行数据类型的强制转换处理，不然反射会报入参类型不匹配的错
     *
     * @param args
     * @param parameterTypes
     * @param genericParameterTypes
     * @return
     */
    private Object[] processArgs(Object[] args, Class<?>[] parameterTypes, Type[] genericParameterTypes) {
        if (args == null || args.length < 1) {
            return args;
        }
        Object[] actual = new Object[args.length];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = TypeUtils.castGeneric(args[i], parameterTypes[i], genericParameterTypes[i]);
        }
        return actual;
    }

    private ProviderMeta findProviderMeta(List<ProviderMeta> providerMetas, String methodSign) {
        Optional<ProviderMeta> optional = providerMetas.stream()
                .filter(providerMeta -> providerMeta.getMethodSign().equals(methodSign))
                .findFirst();
        return optional.orElse(null);
    }

}
