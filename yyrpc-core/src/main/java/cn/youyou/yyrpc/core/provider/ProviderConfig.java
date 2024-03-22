package cn.youyou.yyrpc.core.provider;

import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.registry.zk.ZkRegistryCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class ProviderConfig {

    /**
     * 交由spring容器管理，在类的生命周期内触发类上实现的相关接口和相关注解的背后逻辑
     *
     * @return
     */
    @Bean
    public ProviderBootstrap providerBootstrap() {
        return new ProviderBootstrap();
    }

    @Bean
    public ProviderInvoker providerInvoker(@Autowired ProviderBootstrap providerBootstrap) {
        return new ProviderInvoker(providerBootstrap);
    }

    /**
     * 构造一个容器加载完成，项目启动完毕的触发时机，用于触发自定义启动逻辑
     *
     * @param providerBootstrap
     * @return
     */
    @Bean
    @Order(Integer.MIN_VALUE)
    public ApplicationRunner providerBootstrapRunner(@Autowired ProviderBootstrap providerBootstrap) {
        // 触发ProviderBootstrap的start，用于完成将自身注册进注册中心
        return args -> {
            System.out.println("providerBootstrap starting ...");
            providerBootstrap.start();
            System.out.println("providerBootstrap started ...");
        };
    }

    @Bean
    public RegistryCenter providerRc() {
        return new ZkRegistryCenter();
    }


}
