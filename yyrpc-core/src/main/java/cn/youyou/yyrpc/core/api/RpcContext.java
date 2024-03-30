package cn.youyou.yyrpc.core.api;

import cn.youyou.yyrpc.core.meta.InstanceMeta;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RpcContext {

    private List<Filter> filters;

    private LoadBalancer<InstanceMeta> loadBalancer;

    private Router<InstanceMeta> router;

    private Map<String, String> parameters = new HashMap<>();

}
