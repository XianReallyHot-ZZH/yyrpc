package cn.youyou.yyrpc.core.meta;

import com.alibaba.fastjson.JSON;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于表达服务定义的元信息
 * 后续要增强的属性都添加在这里
 */
@Data
@Builder
public class ServiceMeta {

    private String app;
    private String namespace;
    private String env;
    private String name;

    private Map<String, String> parameters = new HashMap<>();

    /**
     * 用于注册到注册中心的代表服务的路径格式
     * @return
     */
    public String toPath() {
        return String.format("%s_%s_%s_%s", app, namespace, env, name);
    }

    public String toMetas() {
        return JSON.toJSONString(this.getParameters());
    }

}
