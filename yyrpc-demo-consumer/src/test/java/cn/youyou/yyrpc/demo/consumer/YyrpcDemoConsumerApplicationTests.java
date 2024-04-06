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

    static ApplicationContext context1;
    static ApplicationContext context2;

    static TestZKServer zkServer = new TestZKServer();

    @BeforeAll
    static void init() {
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" =============     ZK2182    ========== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        zkServer.start();

        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" =============      P8094    ========== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        context1 = SpringApplication.run(YyrpcDemoProviderApplication.class,
                "--server.port=8094", "--kkrpc.zkServer=localhost:2182",
                "--logging.level.cn.kimmking.kkrpc=info","--app.metas={dc:'bj',gray:'false',unit:'B001'}");

        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" =============      P8095    ========== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        context2 = SpringApplication.run(YyrpcDemoProviderApplication.class,
                "--server.port=8095", "--kkrpc.zkServer=localhost:2182",
                "--logging.level.cn.kimmking.kkrpc=info","--app.metas={dc:'bj',gray:'false',unit:'B001'}");

    }

    @Test
    void contextLoads() {
        System.out.println(" ===> YyrpcDemoConsumerApplicationTests .... ");
    }

    @AfterAll
    static void destroy() {
        SpringApplication.exit(context1, () -> 1);
        SpringApplication.exit(context2, () -> 1);
        zkServer.stop();
    }

}
