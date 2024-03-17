package cn.youyou.yyrpc.core.cluster;

import cn.youyou.yyrpc.core.api.LoadBalancer;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡
 * @param <T>
 */
public class RandomLoadBalancer<T> implements LoadBalancer<T> {

    Random random = new Random();

    @Override
    public T choose(List<T> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        if (providers.size() == 1) {
            return providers.get(0);
        }
        return providers.get(random.nextInt(providers.size()));
    }
}
