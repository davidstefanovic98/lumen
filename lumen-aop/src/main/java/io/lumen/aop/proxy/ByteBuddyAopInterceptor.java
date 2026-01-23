package io.lumen.aop.proxy;

import io.lumen.aop.ReflectiveMethodInvocation;
import io.lumen.core.interceptor.MethodInterceptor;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;

public class ByteBuddyAopInterceptor {
    private final List<MethodInterceptor> interceptors;
    public ByteBuddyAopInterceptor(List<MethodInterceptor> interceptors) { this.interceptors = interceptors; }

    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args, @SuperCall Callable<?> zuper) throws Throwable {
        return new ReflectiveMethodInvocation(method, args, zuper, interceptors).proceed();
    }
}
