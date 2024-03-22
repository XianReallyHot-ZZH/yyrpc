package cn.youyou.yyrpc.core.registry.zk;

import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.registry.ChangedListener;
import cn.youyou.yyrpc.core.registry.Event;
import lombok.SneakyThrows;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;

public class ZkRegistryCenter implements RegistryCenter {

    private CuratorFramework client = null;

    @Override
    public void start() {
        // 项目启动时，完成客户端相关的初始化工作
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .namespace("yyrpc")
                .retryPolicy(retryPolicy)
                .build();
        System.out.println("===>[ZkRegistryCenter] zk client starting.");
        client.start();
        System.out.println("===>[ZkRegistryCenter] zk client started.");
    }

    @Override
    public void stop() {
        // 项目关闭时，关闭相关的资源
        System.out.println("===>[ZkRegistryCenter] zk client stopping.");
        client.close();
        System.out.println("===>[ZkRegistryCenter] zk client stopped.");
    }

    @Override
    public void register(String service, String instance) {
        // 业务语义上的初始化工作之一，消费端需要将自己注册到ZK上
        String servicePath = "/" + service;
        try {
            // 创建服务路径
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().withMode(CreateMode.PERSISTENT).forPath(servicePath, "service".getBytes());
            }
            // 创建服务提供者的临时节点信息
            String instancePath = servicePath + "/" + instance;
            client.create().withMode(CreateMode.EPHEMERAL).forPath(instancePath, "provider".getBytes());
            System.out.println("===>[ZkRegistryCenter] register to zk, provider: " + instancePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unRegister(String service, String instance) {
        // 业务语义上的资源反注销工作之一，消费端需要将自己从ZK上注销
        String servicePath = "/" + service;

        try {
            // 判断服务还在不在
            if (client.checkExists().forPath(servicePath) == null) {
                return;
            }
            String instancePath = servicePath + "/" + instance;
            client.delete().quietly().forPath(instancePath);
            System.out.println("===>[ZkRegistryCenter] unRegister from zk, provider: " + instancePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> fetchAll(String service) {
        // 业务运行期，注册中心的核心功能之一，提供从ZK上获取相关资源的功能
        String servicePath = "/" + service;
        try {
            List<String> nodes = client.getChildren().forPath(servicePath);
            System.out.println(" ===>[ZkRegistryCenter] service=" + service + ", fetchAll nodes from zk: " + servicePath);
            nodes.forEach(System.out::println);
            return nodes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SneakyThrows
    public void subscribe(String service, ChangedListener listener) {
        // 通过将回调逻辑注册到ZK缓存监控工具上，实现自动监听zk变化，触发相应的业务逻辑
        System.out.println(" ===>[ZkRegistryCenter] 进行挂载, service:" + service);
        final TreeCache cache = TreeCache.newBuilder(client, "/" + service)
                .setCacheData(true)
                .setMaxDepth(2)
                .build();
        cache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                // 监听到变动，进而触发这里的逻辑
                System.out.println(" ===>[ZkRegistryCenter] subscribe from zk, event: " + event);
                List<String> nodes = fetchAll(service);
                // 触发定义好的订阅更新逻辑
                listener.fire(new Event(nodes));
            }
        });
        cache.start();
    }


}
