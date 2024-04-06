package cn.youyou.yyrpc.core.consumer;

import cn.youyou.yyrpc.core.api.*;
import cn.youyou.yyrpc.core.cluster.GrayRouter;
import cn.youyou.yyrpc.core.cluster.RoundRibonLoadBalancer;
import cn.youyou.yyrpc.core.filter.ParameterFilter;
import cn.youyou.yyrpc.core.registry.zk.ZkRegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
@Slf4j
public class ConsumerConfig {

    @Value("${app.grayRatio:0}")
    private int grayRatio;

    @Value("${app.id:app1}")
    private String app;

    @Value("${app.namespace:public}")
    private String namespace;

    @Value("${app.env:dev}")
    private String env;

    @Value("${app.retries:1}")
    private int retries;

    @Value("${app.timeout:1000}")
    private int timeout;

    @Value("${app.faultLimit:10}")
    private int faultLimit;

    @Value("${app.halfOpenInitialDelay:10000}")
    private int halfOpenInitialDelay;

    @Value("${app.halfOpenDelay:60000}")
    private int halfOpenDelay;


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
        return new GrayRouter(grayRatio);
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
        rpcContext.getParameters().put("app.id", app);
        rpcContext.getParameters().put("app.namespace", namespace);
        rpcContext.getParameters().put("app.env", env);
        rpcContext.getParameters().put("app.retries", String.valueOf(retries));
        rpcContext.getParameters().put("app.timeout", String.valueOf(timeout));
        rpcContext.getParameters().put("app.halfOpenInitialDelay", String.valueOf(halfOpenInitialDelay));
        rpcContext.getParameters().put("app.faultLimit", String.valueOf(faultLimit));
        rpcContext.getParameters().put("app.halfOpenDelay", String.valueOf(halfOpenDelay));
        return rpcContext;
    }

}
