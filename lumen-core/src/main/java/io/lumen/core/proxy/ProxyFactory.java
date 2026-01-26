package io.lumen.core.proxy;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.interceptor.MethodInterceptor;

import java.util.AbstractList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Factory for creating proxies for lazy or intercepted objects.
 */

public class ProxyFactory {
    private static final ProxyProvider provider = loadProvider();

    private static ProxyProvider loadProvider() {
        try {
            return ServiceLoader.load(ProxyProvider.class).findFirst().orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static ProxyProvider getRequiredProvider() {
        if (provider == null) {
            throw new IllegalStateException("Proxying feature requires the 'lumen-aop' module on the classpath.");
        }
        return provider;
    }

    public static <T> T createLazy(LightContainer container, LightInstance light, Class<T> type) {
        return getRequiredProvider().createLazyProxy(container, light, type);
    }

    public static <T> T createConfigurationProxy(Class<T> configClass, LightContainer container) {
        return getRequiredProvider().createConfigurationProxy(configClass, container);
    }

    public static <T> T createAopProxy(Class<T> type, List<MethodInterceptor> interceptors) {
        return getRequiredProvider().createAopProxy(type, interceptors);
    }

    public static <T> T createInterfaceProxy(Class<T> type, List<MethodInterceptor> interceptors) {
        return getRequiredProvider().createInterfaceProxy(type, interceptors);
    }

    public static List<?> createLazyCollection(LightContainer container, List<LightInstance> elements) {
        return new AbstractList<>() {
            @Override public Object get(int index) { return container.getLight(elements.get(index).getName()); }
            @Override public int size() { return elements.size(); }
        };
    }
}
