package io.lumen.cache;

import io.lumen.cache.annotation.CacheEvict;
import io.lumen.cache.annotation.CachePut;
import io.lumen.cache.annotation.Cacheable;
import io.lumen.core.proxy.ProxyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CacheInterceptorTest {

    private SimpleCacheManager cacheManager;
    private CountingService proxy;
    private CountingService delegate;

    @BeforeEach
    void setUp() {
        cacheManager = new SimpleCacheManager();
        delegate = new CountingService();
        proxy = ProxyFactory.createDelegatingProxy(
                CountingService.class, delegate,
                List.of(new CacheInterceptor(cacheManager))
        );
    }

    // --- @Cacheable ---

    @Test
    void cacheableMissCallsMethod() {
        String result = proxy.findById(1L);
        assertEquals("item-1", result);
        assertEquals(1, delegate.callCount);
    }

    @Test
    void cacheableHitSkipsMethod() {
        proxy.findById(1L);
        proxy.findById(1L);
        assertEquals(1, delegate.callCount);
    }

    @Test
    void cacheableKeyIsolatesEntries() {
        proxy.findById(1L);
        proxy.findById(2L);
        proxy.findById(1L);
        proxy.findById(2L);
        assertEquals(2, delegate.callCount); // each id called once
    }

    @Test
    void cacheableNoArgMethodCachedByMethodName() {
        proxy.findAll();
        proxy.findAll();
        assertEquals(1, delegate.callCount);
    }

    @Test
    void cacheableStoresNullResult() {
        proxy.findNullable(99L); // returns null
        proxy.findNullable(99L);
        assertEquals(1, delegate.callCount); // null was cached, not re-fetched
    }

    // --- @CacheEvict ---

    @Test
    void cacheEvictAllEntriesClearsCache() {
        proxy.findById(1L);
        proxy.findById(2L);
        proxy.evictAll();
        proxy.findById(1L);
        proxy.findById(2L);
        assertEquals(4, delegate.callCount);
    }

    @Test
    void cacheEvictByKeyRemovesOnlyThatEntry() {
        proxy.findById(1L);
        proxy.findById(2L);
        proxy.evictById(1L);
        proxy.findById(1L); // cache miss — re-fetched
        proxy.findById(2L); // cache hit — not re-fetched
        assertEquals(3, delegate.callCount);
    }

    @Test
    void cacheEvictBeforeInvocationRunsBeforeMethod() {
        proxy.findById(1L); // warm up cache
        proxy.evictBeforeInvocation(); // clears before running
        proxy.findById(1L); // cache miss → call
        assertEquals(2, delegate.callCount);
    }

    // --- key collision guard ---

    @Test
    void explicitKeyCollision_typeMismatch_evictsAndReInvokes() {
        // Two methods share the same cache name and both use key="#id"/"#projectId"
        // which resolve to the same Long value. The second call must not throw
        // ClassCastException — it must detect the type mismatch, evict, and re-invoke.
        var collisionDelegate = new CollisionService();
        var collisionProxy = ProxyFactory.createDelegatingProxy(
                CollisionService.class, collisionDelegate,
                List.of(new CacheInterceptor(new SimpleCacheManager())));

        // Warm cache: stores String under key Long(1)
        String str = collisionProxy.findById(1L);
        assertEquals("entity-1", str);
        assertEquals(1, collisionDelegate.findByIdCount);

        // Same cache, same key value → stored type is String, expected List → mismatch
        List<String> list = collisionProxy.findByProject(1L);
        assertEquals(List.of("project-1"), list);
        assertEquals(1, collisionDelegate.findByProjectCount, "findByProject must be called once after evicting the mismatched entry");
    }

    @Test
    void explicitKeyCollision_reverseOrder_typeMismatch_evictsAndReInvokes() {
        var collisionDelegate = new CollisionService();
        var collisionProxy = ProxyFactory.createDelegatingProxy(
                CollisionService.class, collisionDelegate,
                List.of(new CacheInterceptor(new SimpleCacheManager())));

        // Warm cache: stores List under key Long(1)
        collisionProxy.findByProject(1L);
        assertEquals(1, collisionDelegate.findByProjectCount);

        // Now findById expects String but cache holds List → mismatch → re-invoke
        String result = collisionProxy.findById(1L);
        assertEquals("entity-1", result);
        assertEquals(1, collisionDelegate.findByIdCount, "findById must be called once after evicting the mismatched entry");
    }

    @Test
    void defaultKey_differentMethods_neverCollide() {
        // When no explicit key is set, the default key includes ClassName#methodName,
        // so two no-arg methods on the same cache cannot collide.
        var noKeyDelegate = new NoExplicitKeyService();
        var noKeyProxy = ProxyFactory.createDelegatingProxy(
                NoExplicitKeyService.class, noKeyDelegate,
                List.of(new CacheInterceptor(new SimpleCacheManager())));

        noKeyProxy.getConfig();
        noKeyProxy.getSettings();
        noKeyProxy.getConfig();  // hit
        noKeyProxy.getSettings(); // hit

        assertEquals(1, noKeyDelegate.getConfigCount);
        assertEquals(1, noKeyDelegate.getSettingsCount);
    }

    // --- @CachePut ---

    @Test
    void cachePutAlwaysCallsMethodAndUpdatesCache() {
        proxy.findById(1L); // miss, callCount=1
        proxy.update(1L, "updated"); // always runs, callCount=2, cache updated
        String result = proxy.findById(1L); // hit, callCount still 2
        assertEquals("updated", result);
        assertEquals(2, delegate.callCount);
    }

    // --- fixtures ---

    static class CollisionService {
        int findByIdCount = 0;
        int findByProjectCount = 0;

        @Cacheable(value = "shared", key = "#id")
        public String findById(Long id) {
            findByIdCount++;
            return "entity-" + id;
        }

        @Cacheable(value = "shared", key = "#projectId")
        public List<String> findByProject(Long projectId) {
            findByProjectCount++;
            return List.of("project-" + projectId);
        }
    }

    static class NoExplicitKeyService {
        int getConfigCount = 0;
        int getSettingsCount = 0;

        @Cacheable("config")
        public String getConfig() {
            getConfigCount++;
            return "cfg";
        }

        @Cacheable("config")
        public String getSettings() {
            getSettingsCount++;
            return "settings";
        }
    }

    // --- service under test ---

    static class CountingService {
        int callCount = 0;

        @Cacheable(value = "items", key = "#id")
        public String findById(Long id) {
            callCount++;
            return "item-" + id;
        }

        @Cacheable("items")
        public List<String> findAll() {
            callCount++;
            return List.of("a", "b", "c");
        }

        @Cacheable(value = "items", key = "#id")
        public String findNullable(Long id) {
            callCount++;
            return null;
        }

        @CacheEvict(value = "items", allEntries = true)
        public void evictAll() {}

        @CacheEvict(value = "items", key = "#id")
        public void evictById(Long id) {}

        @CacheEvict(value = "items", allEntries = true, beforeInvocation = true)
        public void evictBeforeInvocation() {}

        @CachePut(value = "items", key = "#id")
        public String update(Long id, String value) {
            callCount++;
            return value;
        }
    }
}