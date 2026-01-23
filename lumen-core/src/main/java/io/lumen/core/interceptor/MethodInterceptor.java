package io.lumen.core.interceptor;

public interface MethodInterceptor {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
