package cn.youyou.yyrpc.core.config;

import cn.youyou.yyrpc.core.api.*;
import cn.youyou.yyrpc.core.cluster.GrayRouter;
import cn.youyou.yyrpc.core.cluster.RoundRibonLoadBalancer;
import cn.youyou.yyrpc.core.consumer.ConsumerBootstrap;
import cn.youyou.yyrpc.core.filter.ParameterFilter;
import cn.youyou.yyrpc.core.registry.zk.ZkRegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
@Slf4j
@Import({AppConfigProperties.class, ConsumerConfigProperties.class})
public class ConsumerConfig {

    @Autowired
    AppConfigProperties appConfigProperties;

    @Autowired
    ConsumerConfigProperties consumerConfigProperties;


    @Bean
    ConsumerBootstrap createConsumerBootstrap() {
        return new ConsumerBootstrap();
    }

    /**
     * 消费端rpc相关功能配置加载的触发时机在这里
     * 容器全部加载完毕，进行触发
     *
     * @return
     */
    @Bean
    @Order(value = Integer.MIN_VALUE + 1)
    public ApplicationRunner consumerBootstrapRunner(ConsumerBootstrap consumerBootstrap) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) throws Exception {
                log.info("consumerBootstrap starting ...");
                consumerBootstrap.start();
                log.info("consumerBootstrap started ...");
            }
        };
    }

    /**
     * 临时先这么测
     *
     * @return
     */
    @Bean
    public LoadBalancer loadBalancer() {
        return new RoundRibonLoadBalancer();
//        return new RandomLoadBalancer();
    }

    /**
     * 临时先这么测
     *
     * @return
     */
    @Bean
    public Router router() {
        return new GrayRouter(consumerConfigProperties.getGrayRatio());
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistryCenter registryCenter() {
//        return new RegistryCenter.StaticRegistryCenter(List.of(servers.split(",")));
        return new ZkRegistryCenter();
    }

    @Bean
    public ParameterFilter defaultFilter() {
        return new ParameterFilter();
    }

    @Bean
    public RpcContext createContext(@Autowired Router router,
                                    @Autowired LoadBalancer loadBalancer,
                                    @Autowired List<Filter> filters) {
        RpcContext rpcContext = new RpcContext();
        rpcContext.setLoadBalancer(loadBalancer);
        rpcContext.setRouter(router);
        rpcContext.setFilters(filters);
        rpcContext.getParameters().put("app.id", appConfigProperties.getId());
        rpcContext.getParameters().put("app.namespace", appConfigProperties.getNamespace());
        rpcContext.getParameters().put("app.env", appConfigProperties.getEnv());
        rpcContext.getParameters().put("app.retries", String.valueOf(consumerConfigProperties.getRetries()));
        rpcContext.getParameters().put("app.timeout", String.valueOf(consumerConfigProperties.getTimeout()));
        rpcContext.getParameters().put("app.halfOpenInitialDelay", String.valueOf(consumerConfigProperties.getHalfOpenInitialDelay()));
        rpcContext.getParameters().put("app.faultLimit", String.valueOf(consumerConfigProperties.getFaultLimit()));
        rpcContext.getParameters().put("app.halfOpenDelay", String.valueOf(consumerConfigProperties.getHalfOpenDelay()));
        return rpcContext;
    }

}
