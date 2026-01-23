package io.lumen.aop;

import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;

public class ReflectiveMethodInvocation implements MethodInvocation {
    private final Method method;
    private final Object[] args;
    private final Callable<?> targetCall;
    private final List<MethodInterceptor> interceptors;
    private int currentIndex = -1;

    public ReflectiveMethodInvocation(Method method, Object[] args, Callable<?> targetCall, List<MethodInterceptor> interceptors) {
        this.method = method;
        this.args = args;
        this.targetCall = targetCall;
        this.interceptors = interceptors;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return args;
    }

    @Override
    public Object proceed() throws Throwable {
        if (currentIndex == interceptors.size() - 1) {
            return targetCall.call();
        }
        MethodInterceptor interceptor = interceptors.get(++currentIndex);
        return interceptor.invoke(this);
    }
}
