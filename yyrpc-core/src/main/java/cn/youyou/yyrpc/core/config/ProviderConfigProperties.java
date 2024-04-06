package cn.youyou.yyrpc.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "yyrpc.provider")
public class ProviderConfigProperties {

    // for app provider
    Map<String, String> metas = new HashMap<>();

}
