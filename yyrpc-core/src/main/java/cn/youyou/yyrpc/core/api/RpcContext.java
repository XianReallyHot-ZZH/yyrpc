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

    public static ThreadLocal<Map<String, String>> ContextParameters = new ThreadLocal<>() {
        @Override
        protected Map<String, String> initialValue() {
            return new HashMap<>();
        }
    };

    public static void setContextParameters(String key, String value) {
        ContextParameters.get().put(key, value);
    }

    public static String getContextParameters(String key) {
        return ContextParameters.get().get(key);
    }

    public static String removeContextParameters(String key) {
        return ContextParameters.get().remove(key);
    }
}
