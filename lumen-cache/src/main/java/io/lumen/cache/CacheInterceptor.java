package io.lumen.cache;

import io.lumen.cache.annotation.CacheEvict;
import io.lumen.cache.annotation.CachePut;
import io.lumen.cache.annotation.Cacheable;
import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.util.ReflectionUtil;

import java.lang.reflect.Method;

public class CacheInterceptor implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(CacheInterceptor.class);

    private final CacheManager cacheManager;
    private final SimpleKeyGenerator keyGenerator;

    public CacheInterceptor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.keyGenerator = new SimpleKeyGenerator();
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object[] args = invocation.getArguments();

        Cacheable cacheable = ReflectionUtil.findAnnotation(method, Cacheable.class);
        if (cacheable != null) {
            return handleCacheable(invocation, cacheable, method, args);
        }

        CacheEvict evict = ReflectionUtil.findAnnotation(method, CacheEvict.class);
        if (evict != null) {
            return handleCacheEvict(invocation, evict, method, args);
        }

        CachePut put = ReflectionUtil.findAnnotation(method, CachePut.class);
        if (put != null) {
            return handleCachePut(invocation, put, method, args);
        }

        return invocation.proceed();
    }

    private Object handleCacheable(MethodInvocation invocation, Cacheable ann, Method method, Object[] args) throws Throwable {
        Cache cache = cacheManager.getCache(ann.value());
        Object key = keyGenerator.resolveKey(ann.key(), method, null, args);

        Cache.ValueWrapper cached = cache.get(key);
        if (cached != null) {
            Object value = cached.get();
            if (value != null && !isCompatibleReturnType(method.getReturnType(), value)) {
                logger.warn("Cache '{}' key '{}': stored {} is incompatible with {}.{}() return type {} " +
                                "— possible key collision; evicting stale entry and re-invoking",
                        ann.value(), key, value.getClass().getSimpleName(),
                        method.getDeclaringClass().getSimpleName(), method.getName(),
                        method.getReturnType().getSimpleName());
                cache.evict(key);
            } else {
                return value;
            }
        }

        Object result = invocation.proceed();
        cache.put(key, result);
        return result;
    }

    private static boolean isCompatibleReturnType(Class<?> returnType, Object value) {
        if (returnType.isPrimitive()) return true;
        return returnType.isInstance(value);
    }

    private Object handleCacheEvict(MethodInvocation invocation, CacheEvict ann, Method method, Object[] args) throws Throwable {
        Cache cache = cacheManager.getCache(ann.value());

        if (ann.beforeInvocation()) {
            doEvict(cache, ann, method, args);
        }

        Object result = invocation.proceed();

        if (!ann.beforeInvocation()) {
            doEvict(cache, ann, method, args);
        }

        return result;
    }

    private void doEvict(Cache cache, CacheEvict ann, Method method, Object[] args) {
        if (ann.allEntries()) {
            cache.clear();
        } else {
            Object key = keyGenerator.resolveKey(ann.key(), method, null, args);
            cache.evict(key);
        }
    }

    private Object handleCachePut(MethodInvocation invocation, CachePut ann, Method method, Object[] args) throws Throwable {
        Object result = invocation.proceed();
        Cache cache = cacheManager.getCache(ann.value());
        Object key = keyGenerator.resolveKey(ann.key(), method, null, args);
        cache.put(key, result);
        return result;
    }
}