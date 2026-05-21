package io.lumen.core.proxy;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.interceptor.MethodInterceptor;

import java.util.List;

public interface ProxyProvider {
    <T> T createLazyProxy(LightContainer container, LightInstance light, Class<T> type);
    <T> T createAopProxy(Class<T> type, List<MethodInterceptor> interceptors);
    <T> T createConfigurationProxy(Class<T> configClass, LightContainer container);
    <T> T createInterfaceProxy(Class<T> type, List<MethodInterceptor> interceptors);

    /**
     * Creates a subclass proxy that delegates all method calls to the given target instance.
     * Unlike createAopProxy, the target's injected fields are preserved — the proxy
     * intercepts calls and routes them to the original object.
     */
    <T> T createDelegatingProxy(Class<T> type, T target, List<MethodInterceptor> interceptors);
}
