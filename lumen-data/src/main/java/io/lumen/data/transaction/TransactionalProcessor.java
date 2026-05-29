package io.lumen.data.transaction;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.proxy.ProxyFactory;
import io.lumen.data.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

public class TransactionalProcessor implements LightProcessor {

    private final LightContainer container;

    public TransactionalProcessor(LightContainer container) {
        this.container = container;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        Class<?> type = instance.getClass();
        if (!needsProxy(type)) return instance;

        @SuppressWarnings("unchecked")
        Class<Object> rawType = (Class<Object>) type;
        return ProxyFactory.createDelegatingProxy(rawType, instance,
                List.of(new TransactionalInterceptor(container, instance)));
    }

    private boolean needsProxy(Class<?> type) {
        if (type.isAnnotationPresent(Transactional.class)) return true;
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Transactional.class)) return true;
        }
        return false;
    }
}