package cn.youyou.yyrpc.core.util;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 类扫描技术
 * 不依赖于JVM加载的方案
 */
public class ScanPackagesUtils {
    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    public static List<Class<?>> scanPackages(String[] packages, Predicate<Class<?>> predicate) {
        List<Class<?>> result = new ArrayList<>();
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
        for (String basePackage : packages) {
            if (StringUtils.isBlank(basePackage)) {
                continue;
            }
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage)) +
                    "/" +
                    DEFAULT_RESOURCE_PATTERN;
            System.out.println("packageSearchPath=" + packageSearchPath);
            try {
                Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
                for (Resource resource : resources) {
                    System.out.println(" resource: " + resource.getFilename());
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    String className = metadataReader.getClassMetadata().getClassName();
                    Class<?> clazz = Class.forName(className);
                    if (predicate.test(clazz)) {
                        result.add(clazz);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static void main(String[] args) {
        String packages = "cn.youyou.yyrpc";

        System.out.println(" 1. *********** ");
        System.out.println(" => scan all classes for packages: " + packages);
        List<Class<?>> classes = scanPackages(packages.split(","), p -> true);
        System.out.println("scan result:");
        classes.forEach(System.out::println);

        System.out.println();
        System.out.println(" 2. *********** ");
        System.out.println(" => scan all classes with @Configuration for packages: " + packages);
        List<Class<?>> classesWithConfig = scanPackages(packages.split(","),
                p -> Arrays.stream(p.getAnnotations())
                        .anyMatch(a -> a.annotationType().equals(Configuration.class)));
        System.out.println("scan result:");
        classesWithConfig.forEach(System.out::println);
    }

}
