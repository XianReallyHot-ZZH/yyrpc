package cn.youyou.yyrpc.core.provider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {

    /**
     * 交由spring容器管理，在类的生命周期内触发类上实现的相关接口和相关注解的背后逻辑
     * @return
     */
    @Bean
    public ProviderBootstrap providerBootstrap() {
        return new ProviderBootstrap();
    }

}
