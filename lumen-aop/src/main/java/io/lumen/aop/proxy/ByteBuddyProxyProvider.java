package io.lumen.aop.proxy;

import io.lumen.core.annotation.Light;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.proxy.ProxyProvider;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.List;

public class ByteBuddyProxyProvider implements ProxyProvider {
    public ByteBuddyProxyProvider() {}

    @Override
    public <T> T createLazyProxy(LightContainer container, LightInstance light, Class<T> type) {
        return createProxy(type, new ByteBuddyLazyInterceptor(container, light));
    }

    @Override
    public <T> T createAopProxy(Class<T> type, List<MethodInterceptor> interceptors) {
        return createProxy(type, new ByteBuddyAopInterceptor(interceptors));
    }

    @Override
    public <T> T createConfigurationProxy(Class<T> configClass, LightContainer container) {
        try {
            return new ByteBuddy()
                    .subclass(configClass)
                    .method(ElementMatchers.isAnnotatedWith(Light.class))
                    .intercept(MethodDelegation.to(new ByteBuddyConfigInterceptor(container)))
                    .make()
                    .load(configClass.getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Configuration proxy creation failed", e);
        }
    }

    @Override
    public <T> T createInterfaceProxy(Class<T> type, List<MethodInterceptor> interceptors) {
        return createProxy(type, new ByteBuddyInterfaceInterceptor(interceptors));
    }

    private <T> T createProxy(Class<T> type, Object interceptor) {
        try {
            return new ByteBuddy()
                    .subclass(type)
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(interceptor))
                    .make()
                    .load(type.getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Proxy creation failed", e);
        }
    }
}
