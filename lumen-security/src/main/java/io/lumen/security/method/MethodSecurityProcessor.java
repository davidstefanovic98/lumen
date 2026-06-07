package io.lumen.security.method;

import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.proxy.ProxyFactory;
import io.lumen.security.annotation.PostAuthorize;
import io.lumen.security.annotation.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

public class MethodSecurityProcessor implements LightProcessor {

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        Class<?> type = instance.getClass();
        if (!hasSecurityAnnotation(type)) return instance;

        @SuppressWarnings("unchecked")
        Class<Object> rawType = (Class<Object>) type;
        Object proxy = ProxyFactory.createDelegatingProxy(rawType, instance, List.of(new MethodSecurityInterceptor()));
        return proxy;
    }

    private boolean hasSecurityAnnotation(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            if (current.isAnnotationPresent(PreAuthorize.class))  return true;
            if (current.isAnnotationPresent(PostAuthorize.class)) return true;
            for (Method m : current.getDeclaredMethods()) {
                if (m.isAnnotationPresent(PreAuthorize.class))  return true;
                if (m.isAnnotationPresent(PostAuthorize.class)) return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }
}