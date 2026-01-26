package io.lumen.aop.proxy;

import io.lumen.aop.ReflectiveMethodInvocation;
import io.lumen.core.interceptor.MethodInterceptor;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.Method;
import java.util.List;

public class ByteBuddyInterfaceInterceptor {
    private final List<MethodInterceptor> interceptors;

    public ByteBuddyInterfaceInterceptor(List<MethodInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args) throws Throwable {
        return new ReflectiveMethodInvocation(method, args, () -> {
            throw new UnsupportedOperationException("No implementation provided for " + method.getName());
        }, interceptors).proceed();
    }
}
