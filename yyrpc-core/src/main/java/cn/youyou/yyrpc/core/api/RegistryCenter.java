package cn.youyou.yyrpc.core.api;

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
    void register(String service, String instance);
    void unRegister(String service, String instance);

    /**
     * 提供给consumer侧的功能
     */
    List<String> fetchAll(String service);

    class StaticRegistryCenter implements RegistryCenter {

        private List<String> providers;

        public StaticRegistryCenter(List<String> providers) {
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
        public void register(String service, String instance) {

        }

        @Override
        public void unRegister(String service, String instance) {

        }

        @Override
        public List<String> fetchAll(String service) {
            return providers;
        }
    }

}
