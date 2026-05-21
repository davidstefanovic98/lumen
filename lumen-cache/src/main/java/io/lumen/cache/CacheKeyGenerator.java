package io.lumen.cache;

import java.lang.reflect.Method;

public interface CacheKeyGenerator {

    Object generate(Object target, Method method, Object... params);
}