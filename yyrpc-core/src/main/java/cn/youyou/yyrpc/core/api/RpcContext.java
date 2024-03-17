package cn.youyou.yyrpc.core.api;

import lombok.Data;

import java.util.List;

@Data
public class RpcContext {

    private List<Filter> filters;

    private LoadBalancer loadBalancer;

    private Router router;

}
