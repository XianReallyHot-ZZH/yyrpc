package cn.youyou.yyrpc.core.consumer;

import cn.youyou.yyrpc.core.api.LoadBalancer;
import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.api.Router;
import cn.youyou.yyrpc.core.cluster.RoundRibonLoadBalancer;
import cn.youyou.yyrpc.core.registry.zk.ZkRegistryCenter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class ConsumerConfig {

    @Bean
    ConsumerBootstrap createConsumerBootstrap() {
        return new ConsumerBootstrap();
    }

    /**
     * 消费端rpc相关功能配置加载的触发时机在这里
     * 容器全部加载完毕，进行触发
     * @return
     */
    @Bean
    @Order(value = Integer.MIN_VALUE)
    public ApplicationRunner consumerBootstrapRunner(ConsumerBootstrap consumerBootstrap) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) throws Exception {
                System.out.println("consumerBootstrap starting ...");
                consumerBootstrap.start();
                System.out.println("consumerBootstrap started ...");
            }
        };
    }

    /**
     * 临时先这么测
     * @return
     */
    @Bean
    public LoadBalancer loadBalancer() {
        return new RoundRibonLoadBalancer();
//        return new RandomLoadBalancer();
    }

    /**
     * 临时先这么测
     * @return
     */
    @Bean
    public Router router() {
        return Router.Default;
    }

    @Bean
    public RegistryCenter registryCenter() {
//        return new RegistryCenter.StaticRegistryCenter(List.of(servers.split(",")));
        return new ZkRegistryCenter();
    }

}
