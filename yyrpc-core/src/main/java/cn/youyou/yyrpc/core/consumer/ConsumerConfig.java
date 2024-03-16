package cn.youyou.yyrpc.core.consumer;

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
                System.out.println("consumerBootstrap end ...");
            }
        };
    }

}
