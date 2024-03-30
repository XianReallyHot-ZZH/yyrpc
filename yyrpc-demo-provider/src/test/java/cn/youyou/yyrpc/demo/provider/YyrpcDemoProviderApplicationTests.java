package cn.youyou.yyrpc.demo.provider;

import cn.youyou.yyrpc.core.test.TestZKServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class YyrpcDemoProviderApplicationTests {

    static TestZKServer zkServer = new TestZKServer();

    @BeforeAll
    static void init() {
        zkServer.start();
    }

    @Test
    void contextLoads() {
        System.out.println(" ===> KkrpcDemoProviderApplicationTests .... ");
    }

    @AfterAll
    static void destroy() {
        zkServer.stop();
    }

}
