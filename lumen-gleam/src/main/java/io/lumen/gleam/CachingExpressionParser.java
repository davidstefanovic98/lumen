package io.lumen.gleam;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorator that caches parsed {@link Expression} objects by their source string.
 * Since expressions are immutable, the same instance is safe to reuse across threads.
 * This avoids re-parsing the same string on every method invocation (e.g. every
 * {@code @PreAuthorize} check or {@code @Cacheable} key resolution).
 */
public class CachingExpressionParser implements ExpressionParser {

    private final ExpressionParser delegate;
    private final ConcurrentHashMap<String, Expression> cache = new ConcurrentHashMap<>();

    public CachingExpressionParser(ExpressionParser delegate) {
        this.delegate = delegate;
    }

    @Override
    public Expression parse(String expression) {
        return cache.computeIfAbsent(expression, delegate::parse);
    }

    public int cacheSize() {
        return cache.size();
    }

    public void clearCache() {
        cache.clear();
    }
}