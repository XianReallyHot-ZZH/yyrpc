package cn.youyou.yyrpc.demo.consumer;

import cn.youyou.yyrpc.core.test.TestZKServer;
import cn.youyou.yyrpc.demo.provider.YyrpcDemoProviderApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = {YyrpcDemoConsumerApplication.class})
class YyrpcDemoConsumerApplicationTests {

    static ApplicationContext context;

    static TestZKServer zkServer = new TestZKServer();

    @BeforeAll
    static void init() {
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");

        zkServer.start();

        context = SpringApplication.run(YyrpcDemoProviderApplication.class,
                "--server.port=8089", "--kkrpc.zkServer=localhost:2182",
                "--logging.level.cn.kimmking.kkrpc=info");

    }

    @Test
    void contextLoads() {
        System.out.println(" ===> YyrpcDemoConsumerApplicationTests .... ");
    }

    @AfterAll
    static void destroy() {
        SpringApplication.exit(context, () -> 1);
        zkServer.stop();
    }

}
