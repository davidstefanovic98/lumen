package io.lumen.security.method;

import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.proxy.ProxyFactory;
import io.lumen.core.util.ReflectionUtil;
import io.lumen.security.annotation.PostAuthorize;
import io.lumen.security.annotation.PreAuthorize;

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
        return ReflectionUtil.hasAnnotationInHierarchy(type, PreAuthorize.class, PostAuthorize.class);
    }
}