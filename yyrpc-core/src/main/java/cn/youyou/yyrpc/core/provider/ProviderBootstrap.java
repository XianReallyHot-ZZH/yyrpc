package cn.youyou.yyrpc.core.provider;

import cn.youyou.yyrpc.core.annotation.YYProvider;
import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.meta.ProviderMeta;
import cn.youyou.yyrpc.core.util.MethodUtils;
import cn.youyou.yyrpc.core.util.TypeUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * rpc服务端应用启动类，在项目启动期间负责将服务加载进服务端的存根中
 * rpc服务端应用反射调用功能类，处理rpc请求，调用对应的服务后，负责返回rpc结果
 */
@Data
public class ProviderBootstrap implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * 内部数据格式为：key为接口的全限定名，value为接口中各个方法的ProviderMeta集合
     */
    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    @Value("${server.port}")
    private String port;

    private String instance;

    // 实现了ApplicationContextAware接口，Spring容器会在创建该Bean之后，自动调用该Bean的setApplicationContext()方法，调用该方法时，会将容器本身作为参数传给该方法
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 原理：利用bean的生命周期，在该bean创建后，执行如下用户自定义逻辑
     * 作用：获取服务端服务的bean对象，存进存根中
     */
    @PostConstruct
    public void init() {
        Map<String, Object> providers = applicationContext.getBeansWithAnnotation(YYProvider.class);
        providers.forEach((x, y) -> {
            System.out.println("===> provider beanName:" + x);
        });
        providers.values().forEach(this::put2Skeleton);
    }

    /**
     * 项目启动后，要进行的初始化逻辑
     */
    @SneakyThrows
    public void start() {
        instance = InetAddress.getLocalHost().getHostAddress() + "_" + port;
        skeleton.keySet().forEach(this::registerService);
    }

    /**
     * 项目结束终止时，要进行的资源回收类工作
     */
    @PreDestroy
    public void stop() {
        skeleton.keySet().forEach(this::unRegisterService);
    }

    private void registerService(String service) {
        RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);
        rc.register(service, instance);
    }

    private void unRegisterService(String service) {
        RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);
        rc.unRegister(service, instance);
    }

    private void put2Skeleton(Object provider) {
        // 处理一个类实现多个接口的情况
        Arrays.stream(provider.getClass().getInterfaces()).forEach(itfer -> {
            Arrays.stream(itfer.getDeclaredMethods()).forEach(method -> {
                // 过滤掉Object等的原生方法
                if (MethodUtils.checkLocalMethod(method)) {
                    return;
                }
                // 放入存根
                createProviderMeta(itfer, method, provider);
            });
        });
    }

    private void createProviderMeta(Class<?> itfer, Method method, Object provider) {
        ProviderMeta providerMeta = new ProviderMeta();
        providerMeta.setMethod(method);
        providerMeta.setMethodSign(MethodUtils.methodSign(method));
        providerMeta.setServiceImpl(provider);
        System.out.println(" create a provider: " + providerMeta);
        skeleton.add(itfer.getCanonicalName(), providerMeta);
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
            rpcResponse.setEx(new RuntimeException(e.getMessage()));
        } catch (InvocationTargetException e) {
            throw new RuntimeException(new RuntimeException(e.getTargetException().getMessage()));
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
