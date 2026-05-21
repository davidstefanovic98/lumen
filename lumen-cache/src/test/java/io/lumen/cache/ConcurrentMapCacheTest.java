package io.lumen.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentMapCacheTest {

    private ConcurrentMapCache cache;

    @BeforeEach
    void setUp() {
        cache = new ConcurrentMapCache("test");
    }

    @Test
    void getName() {
        assertEquals("test", cache.getName());
    }

    @Test
    void missReturnsNull() {
        assertNull(cache.get("missing"));
    }

    @Test
    void putAndGet() {
        cache.put("key", "value");
        Cache.ValueWrapper wrapper = cache.get("key");
        assertNotNull(wrapper);
        assertEquals("value", wrapper.get());
    }

    @Test
    void nullValueDistinguishedFromMiss() {
        cache.put("key", null);
        Cache.ValueWrapper wrapper = cache.get("key");
        assertNotNull(wrapper);          // wrapper present → cache hit
        assertNull(wrapper.get());       // actual value is null
    }

    @Test
    void evict() {
        cache.put("key", "value");
        cache.evict("key");
        assertNull(cache.get("key"));
    }

    @Test
    void evictUnknownKeyIsNoOp() {
        assertDoesNotThrow(() -> cache.evict("ghost"));
    }

    @Test
    void clear() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();
        assertNull(cache.get("a"));
        assertNull(cache.get("b"));
    }

    @Test
    void overwriteExistingEntry() {
        cache.put("key", "first");
        cache.put("key", "second");
        assertEquals("second", cache.get("key").get());
    }
}