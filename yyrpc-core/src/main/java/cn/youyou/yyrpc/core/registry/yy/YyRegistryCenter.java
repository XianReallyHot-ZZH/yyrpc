package cn.youyou.yyrpc.core.registry.yy;

import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.meta.InstanceMeta;
import cn.youyou.yyrpc.core.meta.ServiceMeta;
import cn.youyou.yyrpc.core.registry.ChangedListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Slf4j
public class YyRegistryCenter implements RegistryCenter {

    /**
     * TODO：先用一个地址开发完功能，后面改成兼容集群
     */
    @Value("${kkregistry.servers:localhost:8484}")
    private String servers;

    @Override
    public void start() {
        log.info(" ====>>>> [KKRegistry] : start with servers : {}", servers);
    }

    @Override
    public void stop() {

    }

    @Override
    public void register(ServiceMeta service, InstanceMeta instance) {

    }

    @Override
    public void unRegister(ServiceMeta service, InstanceMeta instance) {

    }

    @Override
    public List<InstanceMeta> fetchAll(ServiceMeta service) {
        return null;
    }

    @Override
    public void subscribe(ServiceMeta service, ChangedListener listener) {

    }
}
