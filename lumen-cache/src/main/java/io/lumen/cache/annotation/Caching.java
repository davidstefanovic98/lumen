package io.lumen.cache.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Caching {
    Cacheable[] cacheable() default {};
    CacheEvict[] evict() default {};
    CachePut[] put() default {};
}