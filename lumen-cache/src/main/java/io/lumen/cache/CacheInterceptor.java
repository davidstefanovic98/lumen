package io.lumen.cache;

import io.lumen.cache.annotation.CacheEvict;
import io.lumen.cache.annotation.CachePut;
import io.lumen.cache.annotation.Cacheable;
import io.lumen.cache.annotation.Caching;
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

        Caching caching = ReflectionUtil.findAnnotation(method, Caching.class);
        if (caching != null) {
            return handleCaching(invocation, caching, method, args);
        }

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

    // --- @Caching ---

    private Object handleCaching(MethodInvocation invocation, Caching caching, Method method, Object[] args) throws Throwable {
        // 1. Before-invocation evicts
        for (CacheEvict evict : caching.evict()) {
            if (evict.beforeInvocation()) doEvict(evict, method, args);
        }

        // 2. Check @Cacheable entries — return on first cache hit
        for (Cacheable cacheable : caching.cacheable()) {
            Cache cache = cacheManager.getCache(cacheable.value());
            Object key = keyGenerator.resolveKey(cacheable.key(), method, null, args);
            Cache.ValueWrapper cached = cache.get(key);
            if (cached != null) {
                Object value = cached.get();
                if (value == null || isCompatibleReturnType(method.getReturnType(), value)) {
                    return value;
                }
                cache.evict(key); // type mismatch — treat as miss
            }
        }

        // 3. Invoke
        Object result = invocation.proceed();

        // 4. After-invocation evicts
        for (CacheEvict evict : caching.evict()) {
            if (!evict.beforeInvocation()) doEvict(evict, method, args);
        }

        // 5. @CachePut — always update
        for (CachePut put : caching.put()) {
            Cache cache = cacheManager.getCache(put.value());
            Object key = keyGenerator.resolveKey(put.key(), method, null, args);
            cache.put(key, result);
        }

        // 6. @Cacheable misses — store result now that we have it
        for (Cacheable cacheable : caching.cacheable()) {
            Cache cache = cacheManager.getCache(cacheable.value());
            Object key = keyGenerator.resolveKey(cacheable.key(), method, null, args);
            if (cache.get(key) == null) {
                cache.put(key, result);
            }
        }

        return result;
    }

    // --- @Cacheable ---

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
        return ReflectionUtil.wrapperFor(returnType).isInstance(value);
    }

    // --- @CacheEvict ---

    private Object handleCacheEvict(MethodInvocation invocation, CacheEvict ann, Method method, Object[] args) throws Throwable {
        if (ann.beforeInvocation()) doEvict(ann, method, args);
        Object result = invocation.proceed();
        if (!ann.beforeInvocation()) doEvict(ann, method, args);
        return result;
    }

    private void doEvict(CacheEvict ann, Method method, Object[] args) {
        Object key = ann.allEntries() ? null : keyGenerator.resolveKey(ann.key(), method, null, args);
        for (String cacheName : ann.value()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (ann.allEntries()) {
                cache.clear();
            } else {
                cache.evict(key);
            }
        }
    }

    // --- @CachePut ---

    private Object handleCachePut(MethodInvocation invocation, CachePut ann, Method method, Object[] args) throws Throwable {
        Object result = invocation.proceed();
        Cache cache = cacheManager.getCache(ann.value());
        Object key = keyGenerator.resolveKey(ann.key(), method, null, args);
        cache.put(key, result);
        return result;
    }
}