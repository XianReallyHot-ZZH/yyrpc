package cn.youyou.yyrpc.core.provider;

import cn.youyou.yyrpc.core.annotation.YYProvider;
import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.meta.InstanceMeta;
import cn.youyou.yyrpc.core.meta.ProviderMeta;
import cn.youyou.yyrpc.core.util.MethodUtils;
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

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Map;

/**
 * rpc服务端应用启动类
 * 1、在项目启动期间负责将服务加载进服务端的存根中
 * 2、负责项目启动后的一些初始化工作和项目接受后的资源回收类工作，有点优雅启停的意思
 */
@Data
public class ProviderBootstrap implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private RegistryCenter rc;

    /**
     * 内部数据格式为：key为接口的全限定名，value为接口中各个方法的ProviderMeta集合
     */
    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    @Value("${server.port}")
    private String port;

    private InstanceMeta instance;

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
        rc = applicationContext.getBean(RegistryCenter.class);
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
        instance = InstanceMeta.http(InetAddress.getLocalHost().getHostAddress(), Integer.valueOf(port));
        rc.start();
        skeleton.keySet().forEach(this::registerService);
    }

    /**
     * 项目结束终止时，要进行的资源回收类工作
     */
    @PreDestroy
    public void stop() {
        skeleton.keySet().forEach(this::unRegisterService);
        rc.stop();
    }

    private void registerService(String service) {
        rc.register(service, instance);
    }

    private void unRegisterService(String service) {
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
        ProviderMeta providerMeta = ProviderMeta.builder()
                .method(method)
                .methodSign(MethodUtils.methodSign(method))
                .serviceImpl(provider)
                .build();
        System.out.println(" create a provider: " + providerMeta);
        skeleton.add(itfer.getCanonicalName(), providerMeta);
    }

}
