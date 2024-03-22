package cn.youyou.yyrpc.core.meta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 注册到注册中心的实例的信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstanceMeta {

    private String scheme;      // 服务应用的协议
    private String host;        // 服务应用的host
    private Integer port;       // 服务应用的端口
    private String context;     // 服务应用对应的路径

    private boolean status; // online or offline
    private Map<String, String> parameters;

    public InstanceMeta(String scheme, String host, Integer port, String context) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.context = context;
    }

    /**
     * 对应于注册中心的资源格式
     * @return
     */
    public String toPath() {
        return String.format("%s_%d", host, port);
    }

    public String toUrl() {
        return String.format("%s://%s:%d/%s", scheme, host, port, context);
    }

    /**
     * http类型的服务实例元数据
     * @param host
     * @param port
     * @return
     */
    public static InstanceMeta http(String host, Integer port) {
        return new InstanceMeta("http", host, port, "");
    }

}
