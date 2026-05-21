package io.lumen.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentMapCache implements Cache {

    private static final Object NULL_MARKER = new Object();

    private final String name;
    private final ConcurrentMap<Object, Object> store = new ConcurrentHashMap<>();

    public ConcurrentMapCache(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ValueWrapper get(Object key) {
        Object value = store.get(key);
        if (value == null) return null;
        Object unwrapped = value == NULL_MARKER ? null : value;
        return () -> unwrapped;
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, value == null ? NULL_MARKER : value);
    }

    @Override
    public void evict(Object key) {
        store.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
    }
}