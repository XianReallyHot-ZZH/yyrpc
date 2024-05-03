package cn.youyou.yyrpc.core.registry.yy;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 配合yyregistry注册中心的线程模型，主要是一些定时任务
 * 1、定时检查leader
 * 2、定时发送心跳（provider端）
 * 3、定时检测服务版本信息（consumer端），方便及时拉取服务对应的最新的实例信息
 */
@Slf4j
public class YyHealthChecker {

    ScheduledExecutorService consumerExecutor = null;
    ScheduledExecutorService providerExecutor = null;
    ScheduledExecutorService registryLeaderExecutor = null;

    public void start() {
        log.info(" ====>>>> [YYRegistry] : start with health checker.");
        consumerExecutor = Executors.newScheduledThreadPool(1);
        providerExecutor = Executors.newScheduledThreadPool(1);
        registryLeaderExecutor = Executors.newScheduledThreadPool(1);
    }

    public void stop() {
        log.info(" ====>>>> [YYRegistry] : stop with health checker.");
        gracefulShutdown(consumerExecutor);
        gracefulShutdown(providerExecutor);
        gracefulShutdown(registryLeaderExecutor);
    }

    public void consumerCheck(Callback callback) {
        consumerExecutor.scheduleAtFixedRate(() -> {
            try {
                callback.call();
            } catch (Exception e) {
                log.error(" ====>>>> [YYRegistry] : consumer schedule error.", e);
            }
        }, 1, 5, TimeUnit.SECONDS);
    }

    public void providerCheck(Callback callback) {
        providerExecutor.scheduleAtFixedRate(() -> {
            try {
                callback.call();
            } catch (Exception e) {
                log.error(" ====>>>> [YYRegistry] : provider schedule error.", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void registryLeaderCheck(Callback callback) {
        registryLeaderExecutor.scheduleAtFixedRate(() -> {
            try {
                callback.call();
            } catch (Exception e) {
                log.error(" ====>>>> [YYRegistry] : registry leader schedule error.", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void gracefulShutdown(ScheduledExecutorService executorService) {
        executorService.shutdown();
        try {
            executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
            if (!executorService.isTerminated()) {
                executorService.shutdown();
            }
        } catch (Exception e) {
            log.error(" ====>>>> [YYRegistry] : graceful shutdown error.", e);
        }

    }

    // 回调函数
    public interface Callback {
        void call() throws Exception;
    }


}
