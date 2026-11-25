package io.lumen.core.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class SuppliersTest {

    @Test
    void memoize_defersTheDelegateUntilFirstGet() {
        AtomicInteger calls = new AtomicInteger();

        Suppliers.memoize(calls::incrementAndGet);

        assertEquals(0, calls.get());
    }

    @Test
    void memoize_runsTheDelegateOnceAndCachesTheResult() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Integer> memoized = Suppliers.memoize(calls::incrementAndGet);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, calls.get());
    }

    @Test
    void memoize_cachesANullResultToo() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Object> memoized = Suppliers.memoize(() -> { calls.incrementAndGet(); return null; });

        assertNull(memoized.get());
        assertNull(memoized.get());
        assertEquals(1, calls.get());
    }
}