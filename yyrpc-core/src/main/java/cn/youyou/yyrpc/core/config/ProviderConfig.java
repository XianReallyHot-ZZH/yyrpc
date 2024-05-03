package cn.youyou.yyrpc.core.config;

import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.provider.ProviderBootstrap;
import cn.youyou.yyrpc.core.provider.ProviderInvoker;
import cn.youyou.yyrpc.core.registry.yy.YyRegistryCenter;
import cn.youyou.yyrpc.core.registry.zk.ZkRegistryCenter;
import cn.youyou.yyrpc.core.transport.SpringBootTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

import java.util.Map;

@Configuration
@Slf4j
@Import({AppConfigProperties.class, ProviderConfigProperties.class, SpringBootTransport.class})
public class ProviderConfig {

    @Value("${server.port:8081}")
    private String port;

    @Autowired
    AppConfigProperties appConfigProperties;

    @Autowired
    ProviderConfigProperties providerConfigProperties;

    /**
     * 交由spring容器管理，在类的生命周期内触发类上实现的相关接口和相关注解的背后逻辑
     *
     * @return
     */
    @Bean
    public ProviderBootstrap providerBootstrap() {
        return new ProviderBootstrap(port, appConfigProperties, providerConfigProperties);
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
            log.info("providerBootstrap starting ...");
            providerBootstrap.start();
            log.info("providerBootstrap started, every thing is OK");
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistryCenter providerRc() {
//        return new ZkRegistryCenter();
        return new YyRegistryCenter();
    }


}
