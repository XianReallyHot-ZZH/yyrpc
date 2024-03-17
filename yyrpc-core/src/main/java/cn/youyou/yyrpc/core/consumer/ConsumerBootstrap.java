package cn.youyou.yyrpc.core.consumer;

import cn.youyou.yyrpc.core.annotation.YYConsumer;
import cn.youyou.yyrpc.core.api.LoadBalancer;
import cn.youyou.yyrpc.core.api.RegistryCenter;
import cn.youyou.yyrpc.core.api.Router;
import cn.youyou.yyrpc.core.api.RpcContext;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1、生成添加了@YYConsumer的属性对应接口的代理，并将代理放进消费端代理服务存根中；
 * 2、负责扫描spring容器，从中获取属性上添加了@YYConsumer注解的bean，对其相应的属性进行注入；
 */
@Data
public class ConsumerBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;

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
        LoadBalancer loadBalancer = applicationContext.getBean(LoadBalancer.class);
        Router router = applicationContext.getBean(Router.class);
        RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);
        RpcContext rpcContext = new RpcContext();
        rpcContext.setLoadBalancer(loadBalancer);
        rpcContext.setRouter(router);

        // TODO：优化，扫描
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        long start = System.currentTimeMillis();
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            List<Field> annotatedField = findAnnotatedField(bean.getClass());
            serviceProxyInject(bean, annotatedField, rpcContext, rc);
        }

        System.out.println("complete consumer endpoint config take " + (System.currentTimeMillis() - start) + " ms");
    }


    private List<Field> findAnnotatedField(Class<?> aClass) {
        ArrayList<Field> result = new ArrayList<>();
        while (aClass != null) {
            for (Field field : aClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(YYConsumer.class)) {
                    result.add(field);
                }
            }
            aClass = aClass.getSuperclass();
        }

        return result;
    }

    /**
     * 遍历属性，进行代理注入,顺便完成代码存根管理
     *
     * @param bean
     * @param fields
     */
    private void serviceProxyInject(Object bean, List<Field> fields, RpcContext rpcContext, RegistryCenter registryCenter) {
        fields.forEach(field -> {
            System.out.println(" ===> Consumer(@YYConsumer) beanName: " + bean.getClass().getCanonicalName() + ", FieldName: " + field.getName());
            try {
                Class<?> service = field.getType();
                String serviceCanonicalName = service.getCanonicalName();
                // 存根管理+进行代理生成,这里用动态代理
                Object serviceProxy = stub.computeIfAbsent(serviceCanonicalName, k -> createServiceProxy(service, rpcContext, registryCenter));
                // 反射注入
                field.setAccessible(true);
                field.set(bean, serviceProxy);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * RPC 服务接口的代理生成
     *
     * @param service
     * @return
     */
    private Object createServiceProxy(Class<?> service, RpcContext rpcContext, RegistryCenter registryCenter) {
        return Proxy.newProxyInstance(
                service.getClassLoader(),
                new Class[]{service},
                new YYConsumerInvocationHandler(service, rpcContext, registryCenter.fetchAll(service.getCanonicalName()))
        );
    }
}
