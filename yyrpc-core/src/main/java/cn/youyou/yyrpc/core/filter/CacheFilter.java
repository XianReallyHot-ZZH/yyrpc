package cn.youyou.yyrpc.core.filter;

import cn.youyou.yyrpc.core.api.Filter;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheFilter implements Filter {

    /**
     * todo: 引入guava cache，设置过期时间，容量，退出策略等
     */
    static Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public Object preFilter(RpcRequest request) {
        return cache.getOrDefault(request.toString(), null);
    }

    @Override
    public Object postFilter(RpcRequest request, RpcResponse response, Object result) {
        cache.putIfAbsent(request.toString(), result);
        return result;
    }
}
