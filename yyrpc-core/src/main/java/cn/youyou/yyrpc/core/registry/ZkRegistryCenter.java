package cn.youyou.yyrpc.core.registry;

import cn.youyou.yyrpc.core.api.RegistryCenter;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
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


        return null;
    }


}
