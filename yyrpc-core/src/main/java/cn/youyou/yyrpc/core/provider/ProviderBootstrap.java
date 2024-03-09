package cn.youyou.yyrpc.core.provider;

import cn.youyou.yyrpc.core.annotation.YYProvider;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.converter.json.GsonBuilderUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * rpc服务端应用启动类，在项目启动期间负责将服务加载进服务端的存根中
 * rpc服务端应用反射调用功能类，处理rpc请求，调用对应的服务后，负责返回rpc结果
 */
@Data
public class ProviderBootstrap implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * 内部数据格式为：key为接口的全限定名，value为接口的实现类对象
     */
    private Map<String, Object> skeleton = new HashMap<>();

    // 实现了ApplicationContextAware接口，Spring容器会在创建该Bean之后，自动调用该Bean的setApplicationContext()方法，调用该方法时，会将容器本身作为参数传给该方法
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 原理：利用bean的生命周期，在该bean创建后，执行如下用户自定义逻辑
     * 作用：获取服务端服务的bean对象，存进存根中
     */
    @PostConstruct
    public void buildProviders() {
        Map<String, Object> providers = applicationContext.getBeansWithAnnotation(YYProvider.class);
        providers.forEach((x,y) -> {
            System.out.println("provider beanName:" + x);
        });

        providers.values().forEach(this::put2Skeleton);
    }

    private void put2Skeleton(Object provider) {
        Class<?> itferClazz = provider.getClass().getInterfaces()[0];   // TODO:先简单处理，后面再处理一个类实现多接口的情况
        skeleton.put(itferClazz.getCanonicalName(), provider);
    }

    /**
     * 处理rpc请求，调用对应的服务后，负责返回rpc结果
     * todo:这里面的异常处理功能后续补充
     * @param request
     * @return
     */
    public RpcResponse<?> invoke(RpcRequest request) {
        Object provider = skeleton.get(request.getService());
        try {
            Method method = findMethod(provider.getClass(), request.getMethod());
            Object result = method.invoke(provider, request.getArgs());
            return new RpcResponse<>(true, result);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    private Method findMethod(Class<?> aClass, String methodName) {
        for (Method method : aClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
}
