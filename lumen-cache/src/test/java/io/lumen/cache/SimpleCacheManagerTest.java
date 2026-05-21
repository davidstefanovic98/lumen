package io.lumen.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleCacheManagerTest {

    private SimpleCacheManager manager;

    @BeforeEach
    void setUp() {
        manager = new SimpleCacheManager();
    }

    @Test
    void autoCreatesCache() {
        Cache cache = manager.getCache("users");
        assertNotNull(cache);
        assertEquals("users", cache.getName());
    }

    @Test
    void returnsSameInstanceForSameName() {
        Cache first  = manager.getCache("users");
        Cache second = manager.getCache("users");
        assertSame(first, second);
    }

    @Test
    void separateCachesAreIndependent() {
        manager.getCache("a").put("key", "value-a");
        manager.getCache("b").put("key", "value-b");
        assertEquals("value-a", manager.getCache("a").get("key").get());
        assertEquals("value-b", manager.getCache("b").get("key").get());
    }

    @Test
    void getCacheNamesReflectsCreatedCaches() {
        manager.getCache("x");
        manager.getCache("y");
        assertTrue(manager.getCacheNames().containsAll(java.util.List.of("x", "y")));
    }
}