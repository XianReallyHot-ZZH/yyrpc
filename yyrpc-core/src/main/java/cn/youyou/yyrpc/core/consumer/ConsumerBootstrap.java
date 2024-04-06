package cn.youyou.yyrpc.core.consumer;

import cn.youyou.yyrpc.core.annotation.YYConsumer;
import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.api.RpcContext;
import cn.youyou.yyrpc.core.meta.InstanceMeta;
import cn.youyou.yyrpc.core.meta.ServiceMeta;
import cn.youyou.yyrpc.core.util.MethodUtils;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1、生成添加了@YYConsumer的属性对应接口的代理，并将代理放进消费端代理服务存根中；
 * 2、负责扫描spring容器，从中获取属性上添加了@YYConsumer注解的bean，对其相应的属性进行注入；
 */
@Data
@Slf4j
public class ConsumerBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;

    RegistryCenter rc;

    // 服务接口代码存根，key为接口的全限定名，value为相应接口的代理类
    private Map<String, Object> stub = new HashMap<>();


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 完成下面两个功能：
     * 1、生成添加了@YYConsumer的属性对应接口的代理，并将代理放进消费端代理服务存根中；
     * 2、负责扫描spring容器，从中获取属性上添加了@YYConsumer注解的bean，对其相应的属性进行注入；
     */
    public void start() {
        rc = applicationContext.getBean(RegistryCenter.class);
        rc.start();
        RpcContext rpcContext = applicationContext.getBean(RpcContext.class);

        // TODO：优化，扫描
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        long start = System.currentTimeMillis();
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            List<Field> annotatedField = MethodUtils.findAnnotatedField(bean.getClass(), YYConsumer.class);
            serviceProxyInject(bean, annotatedField, rpcContext, rc);
        }

        log.info("complete consumer endpoint config take " + (System.currentTimeMillis() - start) + " ms");
    }

    @PreDestroy
    public void stop() {
        rc.stop();
    }

    /**
     * 遍历属性，进行代理注入,顺便完成代码存根管理
     *
     * @param bean
     * @param fields
     */
    private void serviceProxyInject(Object bean, List<Field> fields, RpcContext rpcContext, RegistryCenter registryCenter) {
        fields.forEach(field -> {
            Class<?> service = field.getType();
            String serviceCanonicalName = service.getCanonicalName();
            log.info(" ===> Consumer(@YYConsumer) beanName: " + serviceCanonicalName + ", FieldName: " + field.getName());
            try {
                // 存根管理+进行代理生成,这里用动态代理
                Object serviceProxy = stub.computeIfAbsent(serviceCanonicalName, k -> createServiceFromRegistryCenter(service, rpcContext, registryCenter));
                // 反射注入
                field.setAccessible(true);
                field.set(bean, serviceProxy);
            } catch (Exception ex) {
                // ignore and print it
                log.warn(" ==> Field[{}.{}] create consumer failed.", serviceCanonicalName, field.getName());
                log.error("Ignore and print it as: ", ex);
            }
        });
    }

    private Object createServiceFromRegistryCenter(Class<?> service, RpcContext rpcContext, RegistryCenter registryCenter) {
        String serviceName = service.getCanonicalName();
        ServiceMeta serviceMeta = ServiceMeta.builder()
                .app(rpcContext.param("app.id"))
                .namespace(rpcContext.param("app.namespace"))
                .env(rpcContext.param("app.env"))
                .name(serviceName).build();
        List<InstanceMeta> providers = registryCenter.fetchAll(serviceMeta);
        log.info(" ===> map to providers: ");
        providers.forEach(System.out::println);
        // 挂载监听
        registryCenter.subscribe(serviceMeta, event -> {
            providers.clear();
            providers.addAll(event.getData());
        });
        return createServiceProxy(service, rpcContext, providers);
    }

    /**
     * RPC 服务接口的代理生成
     *
     * @param service
     * @return
     */
    private Object createServiceProxy(Class<?> service, RpcContext rpcContext, List<InstanceMeta> providers) {
        return Proxy.newProxyInstance(
                service.getClassLoader(),
                new Class[]{service},
                new YYConsumerInvocationHandler(service, rpcContext, providers)
        );
    }
}
