package io.lumen.cache;

import io.lumen.cache.annotation.CacheEvict;
import io.lumen.cache.annotation.CachePut;
import io.lumen.cache.annotation.Cacheable;
import io.lumen.cache.annotation.Caching;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.proxy.ProxyFactory;
import io.lumen.core.util.ReflectionUtil;

import java.util.List;

public class CacheProcessor implements LightProcessor {

    private final CacheManager cacheManager;

    public CacheProcessor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        if (!hasCacheAnnotation(instance.getClass())) return instance;

        @SuppressWarnings("unchecked")
        Class<Object> type = (Class<Object>) instance.getClass();
        return ProxyFactory.createDelegatingProxy(type, instance, List.of(new CacheInterceptor(cacheManager)));
    }

    private boolean hasCacheAnnotation(Class<?> type) {
        return ReflectionUtil.hasAnnotationInHierarchy(type,
                Cacheable.class, CacheEvict.class, CachePut.class, Caching.class);
    }
}