package io.lumen.cache.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {
    String value();
    String key() default "";
    boolean allEntries() default false;
    boolean beforeInvocation() default false;
}