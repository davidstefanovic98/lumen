package io.lumen.aop.proxy;

import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * ByteBuddy interceptor that routes all method calls to the original target instance,
 * running the interceptor chain before and after. Unlike AopInterceptor, the target
 * retains all its injected fields — the proxy is purely a dispatch shell.
 */
class ByteBuddyDelegatingInterceptor<T> {

    private final T target;
    private final List<MethodInterceptor> interceptors;

    ByteBuddyDelegatingInterceptor(T target, List<MethodInterceptor> interceptors) {
        this.target = target;
        this.interceptors = interceptors;
    }

    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args) throws Throwable {
        int[] pos = {0};

        MethodInvocation invocation = new MethodInvocation() {
            @Override public Method getMethod()    { return method; }
            @Override public Object[] getArguments() { return args; }

            @Override
            public Object proceed() throws Throwable {
                if (pos[0] < interceptors.size()) {
                    return interceptors.get(pos[0]++).invoke(this);
                }
                // End of chain — call the real target
                try {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        };

        return invocation.proceed();
    }
}
