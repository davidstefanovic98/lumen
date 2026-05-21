package io.lumen.cache;

import io.lumen.cache.annotation.CacheEvict;
import io.lumen.cache.annotation.CachePut;
import io.lumen.cache.annotation.Cacheable;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.proxy.ProxyFactory;

import java.lang.reflect.Method;
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
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Cacheable.class)
                        || method.isAnnotationPresent(CacheEvict.class)
                        || method.isAnnotationPresent(CachePut.class)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }
}