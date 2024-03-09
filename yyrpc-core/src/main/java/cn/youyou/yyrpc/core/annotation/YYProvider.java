package cn.youyou.yyrpc.core.annotation;

import java.lang.annotation.*;

/**
 * 服务类注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface YYProvider {
}
