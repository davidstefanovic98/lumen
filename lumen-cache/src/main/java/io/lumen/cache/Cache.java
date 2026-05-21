package io.lumen.cache;

public interface Cache {

    String getName();

    ValueWrapper get(Object key);

    void put(Object key, Object value);

    void evict(Object key);

    void clear();

    interface ValueWrapper {
        Object get();
    }
}