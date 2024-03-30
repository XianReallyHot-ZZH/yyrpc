package cn.youyou.yyrpc.demo.provider;

import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.provider.ProviderBootstrap;
import cn.youyou.yyrpc.core.provider.ProviderConfig;
import cn.youyou.yyrpc.core.provider.ProviderInvoker;
import cn.youyou.yyrpc.demo.api.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@Import({ProviderConfig.class})     // @Import可以引入一个@Configuration修饰的类(引入配置类)，从而让配置类生效(把配置类下的所有Bean添加到IOC容器里面去)，配置类不在当前应用包结构下需要这么做
public class YyrpcDemoProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(YyrpcDemoProviderApplication.class, args);
    }

    @Autowired
    public ProviderInvoker providerInvoker;

    /**
     * 模拟一个服务提供端接受基于http协议远程调用的入口
     * @param request
     * @return
     */
    @RequestMapping("/")
    public RpcResponse<?> invoke(@RequestBody RpcRequest request) {
        return providerInvoker.invoke(request);
    }

    @Autowired
    private UserService userService;

    // 模拟故障恢复和发生
    @RequestMapping("/ports")
    public RpcResponse<String> ports(@RequestParam("ports") String ports) {
        userService.setTimeoutPorts(ports);
        RpcResponse<String> response = new RpcResponse<>();
        response.setStatus(true);
        response.setData("OK:" + ports);
        return response;
    }


}
