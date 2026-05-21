package io.lumen.aop.proxy;

import io.lumen.core.annotation.Light;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.proxy.ProxyProvider;
import io.lumen.core.interceptor.MethodInvocation;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ByteBuddyProxyProvider implements ProxyProvider {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

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
                    .load(configClass.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
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

    @Override
    public <T> T createDelegatingProxy(Class<T> type, T target, List<MethodInterceptor> interceptors) {
        try {
            return new ByteBuddy()
                    .subclass(type)
                    .method(ElementMatchers.isPublic().and(ElementMatchers.not(ElementMatchers.isStatic())))
                    .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> {
                        int[] pos = {0};
                        // Resolve the method on the ORIGINAL target class so that
                        // interceptors (e.g. MethodSecurityInterceptor) can find annotations
                        // like @PreAuthorize that exist on the target but not on the proxy.
                        Method targetMethod;
                        try {
                            targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
                        } catch (NoSuchMethodException e) {
                            targetMethod = method;
                        }
                        final Method resolvedMethod = targetMethod;
                        MethodInvocation invocation = new MethodInvocation() {
                            @Override public Method getMethod()      { return resolvedMethod; }
                            @Override public Object[] getArguments() { return args != null ? args : new Object[0]; }
                            @Override public Object proceed() throws Throwable {
                                if (pos[0] < interceptors.size()) return interceptors.get(pos[0]++).invoke(this);
                                try {
                                    // method is declared on the proxy subclass; resolve it on the
                                    // original target's class so invoke() works correctly.
                                    resolvedMethod.setAccessible(true);
                                    return resolvedMethod.invoke(target, args);
                                } catch (InvocationTargetException e) {
                                    throw e.getCause();
                                }
                            }
                        };
                        return invocation.proceed();
                    }))
                    .make()
                    .load(type.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Delegating proxy creation failed for: " + type, e);
        }
    }

    private <T> T createProxy(Class<T> type, Object interceptor) {
        try {
            return new ByteBuddy()
                    .subclass(type)
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(interceptor))
                    .make()
                    .load(type.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Proxy creation failed", e);
        }
    }
}
