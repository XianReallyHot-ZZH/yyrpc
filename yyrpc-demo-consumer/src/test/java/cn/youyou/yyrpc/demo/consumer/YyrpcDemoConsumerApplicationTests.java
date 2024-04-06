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
                "--server.port=8094",
                "--yyrpc.zk.server=localhost:2182",
                "--yyrpc.app.env=test",
                "--logging.level.cn.youyou.yyrpc=info",
                "--yyrpc.provider.metas.dc=bj",
                "--yyrpc.provider.metas.gray=false",
                "--yyrpc.provider.metas.unit=B001");

        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" =============      P8095    ========== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        context2 = SpringApplication.run(YyrpcDemoProviderApplication.class,
                "--server.port=8095",
                "--yyrpc.zk.server=localhost:2182",
                "--yyrpc.app.env=test",
                "--logging.level.cn.youyou.yyrpc=info",
                "--yyrpc.provider.metas.dc=bj",
                "--yyrpc.provider.metas.gray=false",
                "--yyrpc.provider.metas.unit=B002");

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
