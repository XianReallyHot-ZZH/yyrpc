package cn.youyou.yyrpc.core.transport;

import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import cn.youyou.yyrpc.core.provider.ProviderInvoker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpringBootTransport {

    @Autowired
    ProviderInvoker providerInvoker;

    @RequestMapping("/yyrpc")
    public RpcResponse<Object> invoke(@RequestBody RpcRequest request) {
        return providerInvoker.invoke(request);
    }

}
