package cn.youyou.yyrpc.core.api;

import cn.youyou.yyrpc.core.meta.InstanceMeta;
import cn.youyou.yyrpc.core.meta.ServiceMeta;
import cn.youyou.yyrpc.core.registry.ChangedListener;

import java.util.List;

public interface RegistryCenter {

    /**
     * 注册中心本身的一些启动和退出的逻辑
     */
    void start();
    void stop();

    /**
     * 提供给provider侧的功能
     */
    void register(ServiceMeta service, InstanceMeta instance);
    void unRegister(ServiceMeta service, InstanceMeta instance);

    /**
     * 提供给consumer侧的功能
     */
    List<InstanceMeta> fetchAll(ServiceMeta service);
    void subscribe(ServiceMeta service, ChangedListener listener);


    class StaticRegistryCenter implements RegistryCenter {

        private List<InstanceMeta> providers;

        public StaticRegistryCenter(List<InstanceMeta> providers) {
            this.providers = providers;
        }

        @Override
        public void start() {
            System.out.println("StaticRegistryCenter start~~~");
        }

        @Override
        public void stop() {
            System.out.println("StaticRegistryCenter stop~~~");
        }

        @Override
        public void register(ServiceMeta service, InstanceMeta instance) {

        }

        @Override
        public void unRegister(ServiceMeta service, InstanceMeta instance) {

        }

        @Override
        public List<InstanceMeta> fetchAll(ServiceMeta service) {
            return providers;
        }

        @Override
        public void subscribe(ServiceMeta service, ChangedListener listener) {

        }
    }

}
