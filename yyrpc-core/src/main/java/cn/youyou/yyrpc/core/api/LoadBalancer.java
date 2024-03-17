package cn.youyou.yyrpc.core.api;

import java.util.List;

public interface LoadBalancer<T> {

    T choose(List<T> providers);

    /**
     * 默认负载均衡：只取第一个
     */
    LoadBalancer Default = providers -> (providers == null || providers.size() < 1) ? null : providers.get(0);

}
