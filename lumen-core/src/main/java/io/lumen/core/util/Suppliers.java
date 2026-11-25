package io.lumen.core.util;

import java.util.function.Supplier;

public final class Suppliers {

    private Suppliers() {}

    /**
     * Wraps a supplier so the delegate runs at most once; the result is cached thereafter.
     *
     * <p>Used by modules that need every light of some SPI type (e.g. {@code TaskDecorator},
     * {@code FilterChainExceptionMatcher}) but are themselves set up during {@code LumenModule.init()},
     * before the container is initialized: collecting the lights eagerly there would miss
     * {@code @Component}s and lights registered by later modules, so the lookup is deferred to
     * first use and then cached.
     */
    public static <T> Supplier<T> memoize(Supplier<T> delegate) {
        return new Supplier<>() {
            private volatile T value;
            private volatile boolean resolved;

            @Override
            public T get() {
                if (!resolved) {
                    synchronized (this) {
                        if (!resolved) {
                            value = delegate.get();
                            resolved = true;
                        }
                    }
                }
                return value;
            }
        };
    }
}