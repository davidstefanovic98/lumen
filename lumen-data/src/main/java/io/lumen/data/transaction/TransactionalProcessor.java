package io.lumen.data.transaction;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.proxy.ProxyFactory;
import io.lumen.data.annotation.Transactional;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

public class TransactionalProcessor implements LightProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransactionalProcessor.class);

    private final LightContainer container;

    public TransactionalProcessor(LightContainer container) {
        this.container = container;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        Class<?> type = instance.getClass();
        if (!needsProxy(type)) return instance;

        Class<?> proxyType = findProxyType(type);
        if (proxyType == null) return instance;

        return ProxyFactory.createInterfaceProxy(
                proxyType,
                List.of(new TransactionalInterceptor(container, instance))
        );
    }

    private boolean needsProxy(Class<?> type) {
        if (type.isAnnotationPresent(Transactional.class)) return true;
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Transactional.class)) return true;
        }
        return false;
    }

    private Class<?> findProxyType(Class<?> type) {
        for (Class<?> iface : type.getInterfaces()) {
            String name = iface.getName();
            if (!name.startsWith("java.") && !name.startsWith("jakarta.") && !name.startsWith("sun.")) {
                return iface;
            }
        }
        log.debug("@Transactional on {} has no effect: the class must implement at least one " +
                "user-defined interface and be injected by that interface type.", type.getName());
        return null;
    }
}