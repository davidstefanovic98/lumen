package io.lumen.it;

import io.lumen.async.AsyncProcessor;
import io.lumen.async.annotation.Async;
import io.lumen.cache.CacheProcessor;
import io.lumen.cache.SimpleCacheManager;
import io.lumen.cache.annotation.Cacheable;
import io.lumen.security.annotation.PreAuthorize;
import io.lumen.security.method.MethodSecurityProcessor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for AsyncProcessor detecting @Async on classes already wrapped by an earlier
 * proxy processor.
 *
 * When CacheProcessor or MethodSecurityProcessor runs first, the instance handed to AsyncProcessor
 * is a ByteBuddy proxy subclass. ByteBuddy does not copy annotations to the generated override
 * methods, so getDeclaredMethods() on the proxy class returns unannotated methods.
 * AsyncProcessor.hasAsyncMethods() must walk the class hierarchy via ReflectionUtil.findAnnotation()
 * to detect @Async through proxy layers — if it uses isAnnotationPresent() directly it will miss
 * the annotation and skip creating the async proxy.
 *
 * Proxy wrapping order under test:
 *   CacheProcessor    (order -1) first  → CacheProxy(real)
 *   AsyncProcessor    (unordered) second → AsyncProxy(CacheProxy(real))
 *
 *   MethodSecurityProcessor (order 0) first  → SecurityProxy(real)
 *   AsyncProcessor          (unordered) second → AsyncProxy(SecurityProxy(real))
 */
class AsyncProcessorPreProxiedTest {

    // --- service used for cache pre-proxy scenario ---

    interface WorkService {
        String findById(Long id);
        CompletableFuture<String> computeAsync(Long id);
    }

    static class WorkServiceImpl implements WorkService {
        volatile String lastThread = null;
        int callCount = 0;

        @Cacheable(value = "work", key = "#id")
        @Override
        public String findById(Long id) {
            callCount++;
            return "work-" + id;
        }

        @Async
        @Override
        public CompletableFuture<String> computeAsync(Long id) {
            lastThread = Thread.currentThread().getName();
            return CompletableFuture.completedFuture("async-" + id);
        }
    }

    // --- service used for security pre-proxy scenario ---

    interface SecuredService {
        String findById(Long id);
        CompletableFuture<String> computeAsync(Long id);
    }

    static class SecuredServiceImpl implements SecuredService {
        volatile String lastThread = null;
        int callCount = 0;

        @PreAuthorize("permitAll()")
        @Override
        public String findById(Long id) {
            callCount++;
            return "secured-" + id;
        }

        @Async
        @Override
        public CompletableFuture<String> computeAsync(Long id) {
            lastThread = Thread.currentThread().getName();
            return CompletableFuture.completedFuture("secured-async-" + id);
        }
    }

    private static ExecutorService namedExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "lumen-async-pool");
            t.setDaemon(true);
            return t;
        });
    }

    // -------------------------------------------------------------------------
    // Cache proxy first, then async proxy
    // -------------------------------------------------------------------------

    @Test
    void asyncMethod_runsOnPoolThread_whenPreProxiedByCache() throws Exception {
        var executor = namedExecutor();
        var cacheProcessor = new CacheProcessor(new SimpleCacheManager());
        var asyncProcessor = new AsyncProcessor(executor, (ex, m, a) -> {}, List::of);

        var delegate = new WorkServiceImpl();
        var afterCache = cacheProcessor.afterInstantiation(null, delegate);
        var afterAsync = asyncProcessor.afterInstantiation(null, afterCache);

        assertInstanceOf(WorkService.class, afterAsync,
                "AsyncProcessor must produce a proxy even when class is already wrapped by CacheProcessor");
        assertNotSame(afterCache, afterAsync,
                "AsyncProcessor must create a new proxy, not return the CacheProcessor proxy unchanged");

        var proxy = (WorkService) afterAsync;
        var callerThread = Thread.currentThread().getName();
        var result = proxy.computeAsync(42L).get(2, TimeUnit.SECONDS);

        assertEquals("async-42", result);
        assertEquals("lumen-async-pool", delegate.lastThread,
                "@Async method must run on the executor pool thread (was: " + delegate.lastThread
                        + ", caller was: " + callerThread + ")");
    }

    @Test
    void cacheProxy_remainsFunctional_throughAsyncProxyLayer() {
        var executor = namedExecutor();
        var cacheProcessor = new CacheProcessor(new SimpleCacheManager());
        var asyncProcessor = new AsyncProcessor(executor, (ex, m, a) -> {}, List::of);

        var delegate = new WorkServiceImpl();
        var proxy = (WorkService) asyncProcessor.afterInstantiation(null,
                cacheProcessor.afterInstantiation(null, delegate));

        assertEquals("work-5", proxy.findById(5L)); // cache miss
        assertEquals("work-5", proxy.findById(5L)); // cache hit
        assertEquals(1, delegate.callCount, "@Cacheable cache hit must not call through to the real method again");

        executor.shutdown();
    }

    // -------------------------------------------------------------------------
    // Security proxy first, then async proxy
    // -------------------------------------------------------------------------

    @Test
    void asyncMethod_runsOnPoolThread_whenPreProxiedBySecurity() throws Exception {
        var executor = namedExecutor();
        var securityProcessor = new MethodSecurityProcessor();
        var asyncProcessor = new AsyncProcessor(executor, (ex, m, a) -> {}, List::of);

        var delegate = new SecuredServiceImpl();
        var afterSecurity = securityProcessor.afterInstantiation(null, delegate);
        var afterAsync = asyncProcessor.afterInstantiation(null, afterSecurity);

        assertInstanceOf(SecuredService.class, afterAsync,
                "AsyncProcessor must produce a proxy even when class is already wrapped by MethodSecurityProcessor");
        assertNotSame(afterSecurity, afterAsync,
                "AsyncProcessor must create a new proxy, not return the SecurityProcessor proxy unchanged");

        var proxy = (SecuredService) afterAsync;
        var callerThread = Thread.currentThread().getName();
        var result = proxy.computeAsync(7L).get(2, TimeUnit.SECONDS);

        assertEquals("secured-async-7", result);
        assertEquals("lumen-async-pool", delegate.lastThread,
                "@Async method must run on the executor pool thread (was: " + delegate.lastThread
                        + ", caller was: " + callerThread + ")");
    }

    // -------------------------------------------------------------------------
    // Regression guard: the broken behaviour
    // -------------------------------------------------------------------------

    @Test
    void hasAsyncMethods_detectsAnnotationThroughProxyHierarchy() {
        var cacheProcessor = new CacheProcessor(new SimpleCacheManager());
        var asyncProcessor = new AsyncProcessor(
                Executors.newSingleThreadExecutor(), (ex, m, a) -> {}, List::of);

        var delegate = new WorkServiceImpl();
        var proxy = cacheProcessor.afterInstantiation(null, delegate);

        // The proxy class's getDeclaredMethods() returns override methods with no annotations.
        boolean annotationVisibleOnProxyClass = false;
        for (var m : proxy.getClass().getDeclaredMethods()) {
            try {
                if (m.isAnnotationPresent(Async.class)) annotationVisibleOnProxyClass = true;
            } catch (Exception ignored) {}
        }
        assertFalse(annotationVisibleOnProxyClass,
                "ByteBuddy proxy override methods must NOT have @Async — this confirms the bug exists at the proxy layer");

        // AsyncProcessor with the fix must still create an async proxy despite the missing annotation on the proxy class.
        var afterAsync = asyncProcessor.afterInstantiation(null, proxy);
        assertNotSame(proxy, afterAsync,
                "AsyncProcessor must detect @Async via ReflectionUtil.findAnnotation (hierarchy walk), not isAnnotationPresent");
    }
}