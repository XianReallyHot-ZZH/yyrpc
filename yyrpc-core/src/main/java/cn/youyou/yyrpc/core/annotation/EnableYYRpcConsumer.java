package cn.youyou.yyrpc.core.annotation;

import cn.youyou.yyrpc.core.config.ConsumerConfig;
import cn.youyou.yyrpc.core.config.ProviderConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Import({ConsumerConfig.class})
public @interface EnableYYRpcConsumer {

}
