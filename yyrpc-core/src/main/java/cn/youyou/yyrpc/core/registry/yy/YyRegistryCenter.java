package cn.youyou.yyrpc.core.registry.yy;

import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.consumer.HttpInvoker;
import cn.youyou.yyrpc.core.meta.InstanceMeta;
import cn.youyou.yyrpc.core.meta.RegistryServerMeta;
import cn.youyou.yyrpc.core.meta.ServiceMeta;
import cn.youyou.yyrpc.core.registry.ChangedListener;
import cn.youyou.yyrpc.core.registry.Event;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

@Slf4j
public class YyRegistryCenter implements RegistryCenter {

    /**
     * 注册中心接口的一些上下文路径
     */
    private static final String REG_PATH = "/reg";
    private static final String UNREG_PATH = "/unreg";
    private static final String FINDALL_PATH = "/findAll";
    private static final String VERSION_PATH = "/version";
    private static final String RENEWS_PATH = "/renews";
    private static final String LEADER_PATH = "/leader";

    /**
     * TODO：先用一个地址开发完功能，后面改成兼容集群
     */
    @Value("${yyregistry.servers}")
    private String servers;

    private List<String> serversList;

    // 注册中心的leader, 例：http://192.168.28.82:8484
    private String leaderServer;

    private Random random = new Random();

    // 线程模型（定时线程）
    YyHealthChecker healthChecker = new YyHealthChecker();

    // provider端实例保持心跳的服务信息
    MultiValueMap<InstanceMeta, ServiceMeta> RENEWS = new LinkedMultiValueMap<>();

    // consumer端，服务的版本信息，用于判定是否需要从注册中心拉取最新的服务实例信息
    Map<String, Long> VERSIONS = new HashMap<>();


    @Override
    public void start() {
        log.info(" ====>>>> [YYRegistry] : start with servers : {}", servers);
        serversList = Arrays.stream(servers.split(",")).toList();
        leaderCheckProcess();

        healthChecker.start();
        // provider端探活
        providerCheck();
        // 注册中心leader探活
        leaderCheck();

    }

    public void leaderCheck() {
        healthChecker.registryLeaderCheck(this::leaderCheckProcess);
    }

    /**
     * 查询当前注册中心集群的leader信息
     * 注意：这个写的不好，先就这么写吧
     */
    private void leaderCheckProcess() {
        boolean leaderCheck = false;
        while (!leaderCheck) {
            // 用主获取集群leader信息
            try {
                if (leaderServer != null) {
                    RegistryServerMeta registryServerMeta = HttpInvoker.httpGet(leaderPath(), RegistryServerMeta.class);
                    if (registryServerMeta != null) {
                        leaderServer = registryServerMeta.getUrl();
                        leaderCheck = true;
                    }
                }
            } catch (Exception e) {
                log.error(" ====>>>> [YYRegistry] : leaderCheck error", e);
            }

            // 随机选个server查询leader
            try {
                if (!leaderCheck) {
                    String server = serversList.get(random.nextInt(serversList.size()));
                    RegistryServerMeta registryServerMeta = HttpInvoker.httpGet("http://" + server + LEADER_PATH, RegistryServerMeta.class);
                    if (registryServerMeta != null) {
                        leaderServer = registryServerMeta.getUrl();
                        leaderCheck = true;
                    }
                }
            } catch (Exception e) {
                log.error(" ====>>>> [YYRegistry] : leaderCheck error", e);
            }
        }
    }

    @Override
    public void stop() {
        log.info(" ====>>>> [YYRegistry] : stop with servers : {}", servers);
        healthChecker.stop();
    }

    /**
     * provider端注册服务和实例
     *
     * @param service
     * @param instance
     */
    @Override
    public void register(ServiceMeta service, InstanceMeta instance) {
        log.info(" ====>>>> [YYRegistry] : register instance {} for {}", instance, service);
        HttpInvoker.httpPost(JSON.toJSONString(instance), regPath(service), InstanceMeta.class);
        log.info(" ====>>>> [YYRegistry] : registered {}", instance);
        RENEWS.add(instance, service);
    }

    @Override
    public void unRegister(ServiceMeta service, InstanceMeta instance) {
        log.info(" ====>>>> [YYRegistry] : unregister instance {} for {}", instance, service);
        HttpInvoker.httpPost(JSON.toJSONString(instance), unRegPath(service), InstanceMeta.class);
        log.info(" ====>>>> [YYRegistry] : unregistered {}", instance);
        RENEWS.remove(instance, service);
    }

    @Override
    public List<InstanceMeta> fetchAll(ServiceMeta service) {
        log.info(" ====>>>> [YYRegistry] : find all instances for {}", service);
        List<InstanceMeta> instances = HttpInvoker.httpGet(findAllPath(service), new TypeReference<List<InstanceMeta>>() {
        });
        log.info(" ====>>>> [KKRegistry] : findAll = {}", instances);
        return instances;
    }

    // provider端的探活
    public void providerCheck() {
        healthChecker.providerCheck(() -> {
            RENEWS.forEach((instance, services) -> {
                Long timestamp = HttpInvoker.httpPost(JSON.toJSONString(instance), renewsPath(services), Long.class);
                log.info(" ====>>>> [YYRegistry] : renew instance {} at {}", instance, timestamp);
            });
        });
    }

    /**
     * consumer端的订阅
     * @param service
     * @param listener
     */
    @Override
    public void subscribe(ServiceMeta service, ChangedListener listener) {
        // 利用定时任务实现订阅（有点捞，但是先这样实现吧）
        healthChecker.consumerCheck(() -> {
            // 对比服务的版本，决定是否拉取更新
            Long localVersion = VERSIONS.getOrDefault(service.toPath(), -1L);
            Long remoteVersion = HttpInvoker.httpGet(versionPath(service), Long.class);
            log.info(" ====>>>> [YYRegistry] : localVersion = {}, remoteVersion = {}", localVersion, remoteVersion);
            if (remoteVersion > localVersion) {
                List<InstanceMeta> instances = fetchAll(service);
                listener.fire(new Event(instances));
                VERSIONS.put(service.toPath(), remoteVersion);
            }
        });
    }

    /**
     * 注册服务对应的请求路径
     *
     * @param service
     * @return
     */
    private String regPath(ServiceMeta service) {
        return path(REG_PATH, service);
    }

    /**
     * 注销服务对应的请求路径
     *
     * @param service
     * @return
     */
    private String unRegPath(ServiceMeta service) {
        return path(UNREG_PATH, service);
    }

    /**
     * 获取服务的所有实例对应的请求路径
     *
     * @param service
     * @return
     */
    private String findAllPath(ServiceMeta service) {
        return path(FINDALL_PATH, service);
    }

    /**
     * 获取服务版本号对应的请求路径
     *
     * @param service
     * @return
     */
    private String versionPath(ServiceMeta service) {
        return path(VERSION_PATH, service);
    }

    /**
     * 实例心跳刷新对应的请求路径
     *
     * @param serviceList
     * @return
     */
    private String renewsPath(List<ServiceMeta> serviceList) {
        return path(RENEWS_PATH, serviceList);
    }

    private String leaderPath() {
        return leaderServer + LEADER_PATH;
    }

    /**
     * 注册中心相关服务的路径拼装
     *
     * @param context 功能路径
     * @param service 代注册的服务
     * @return
     */
    private String path(String context, ServiceMeta service) {
        return leaderServer + context + "?service=" + service.toPath();
    }

    /**
     * 一个实例上可能有多个服务，拼装在一起，批量刷新心跳
     *
     * @param context
     * @param serviceList
     * @return
     */
    private String path(String context, List<ServiceMeta> serviceList) {
        StringBuffer sb = new StringBuffer();
        for (ServiceMeta service : serviceList) {
            sb.append(service.toPath()).append(",");
        }
        String services = sb.toString();
        if (services.endsWith(",")) {
            services = services.substring(0, services.length() - 1);
        }
        log.info(" ====>>>> [YYRegistry] : renew instance for {}", services);
        return leaderServer + context + "?services=" + services;
    }
}
