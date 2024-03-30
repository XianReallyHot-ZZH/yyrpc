package cn.youyou.yyrpc.core.registry.zk;

import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.meta.InstanceMeta;
import cn.youyou.yyrpc.core.meta.ServiceMeta;
import cn.youyou.yyrpc.core.registry.ChangedListener;
import cn.youyou.yyrpc.core.registry.Event;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ZkRegistryCenter implements RegistryCenter {

    @Value("${yyrpc.zkServer}")
    String server;

    @Value("${yyrpc.zkRoot}")
    String root;

    private CuratorFramework client = null;

    private List<TreeCache> caches = new ArrayList<>();

    @Override
    public void start() {
        // 项目启动时，完成客户端相关的初始化工作
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.builder()
                .connectString(server)
                .namespace(root)
                .retryPolicy(retryPolicy)
                .build();
        log.info("===>[ZkRegistryCenter] zk client starting.");
        client.start();
        log.info("===>[ZkRegistryCenter] zk client started.");
    }

    @Override
    public void stop() {
        // 项目关闭时，关闭相关的资源
        log.info("===>[ZkRegistryCenter] zk client stopping.");
        caches.forEach(TreeCache::close);
        client.close();
        log.info("===>[ZkRegistryCenter] zk client stopped.");
    }

    @Override
    public void register(ServiceMeta service, InstanceMeta instance) {
        // 业务语义上的初始化工作之一，消费端需要将自己注册到ZK上
        String servicePath = "/" + service.toPath();
        try {
            // 创建服务路径
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().withMode(CreateMode.PERSISTENT).forPath(servicePath, "service".getBytes());
            }
            // 创建服务提供者的临时节点信息
            String instancePath = servicePath + "/" + instance.toPath();
            client.create().withMode(CreateMode.EPHEMERAL).forPath(instancePath, "provider".getBytes());
            log.info("===>[ZkRegistryCenter] register to zk, provider: " + instancePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unRegister(ServiceMeta service, InstanceMeta instance) {
        // 业务语义上的资源反注销工作之一，消费端需要将自己从ZK上注销
        String servicePath = "/" + service.toPath();

        try {
            // 判断服务还在不在
            if (client.checkExists().forPath(servicePath) == null) {
                return;
            }
            String instancePath = servicePath + "/" + instance.toPath();
            client.delete().quietly().forPath(instancePath);
            log.info("===>[ZkRegistryCenter] unRegister from zk, provider: " + instancePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<InstanceMeta> fetchAll(ServiceMeta service) {
        // 业务运行期，注册中心的核心功能之一，提供从ZK上获取相关资源的功能
        String servicePath = "/" + service.toPath();
        try {
            List<String> nodes = client.getChildren().forPath(servicePath);
            log.info(" ===>[ZkRegistryCenter] service=" + service.getName() + ", fetchAll nodes from zk: " + servicePath);
            nodes.forEach(System.out::println);
            return mapInstances(nodes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<InstanceMeta> mapInstances(List<String> nodes) {
        return nodes.stream().map(x -> {
            String[] strings = x.split("_");
            return InstanceMeta.http(strings[0], Integer.valueOf(strings[1]));
        }).collect(Collectors.toList());

    }

    @Override
    @SneakyThrows
    public void subscribe(ServiceMeta service, ChangedListener listener) {
        // 通过将回调逻辑注册到ZK缓存监控工具上，实现自动监听zk变化，触发相应的业务逻辑
        log.info(" ===>[ZkRegistryCenter] 进行指定服务的监听挂载, service:" + service.toPath());
        final TreeCache cache = TreeCache.newBuilder(client, "/" + service.toPath())
                .setCacheData(true)
                .setMaxDepth(2)
                .build();
        cache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                // 监听到变动，进而触发这里的逻辑
                log.info(" ===>[ZkRegistryCenter] subscribe from zk, event: " + event);
                List<InstanceMeta> nodes = fetchAll(service);
                // 触发定义好的订阅更新逻辑
                listener.fire(new Event(nodes));
            }
        });
        cache.start();
        caches.add(cache);
    }


}
