package io.lumen.security.method;

import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.security.annotation.PostAuthorize;
import io.lumen.security.annotation.PreAuthorize;

import java.lang.reflect.Method;

public class MethodSecurityInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();

        PreAuthorize pre = resolveAnnotation(method, PreAuthorize.class);
        if (pre != null) MethodSecurityExpressionEvaluator.check(pre.value());

        Object result = invocation.proceed();

        PostAuthorize post = resolveAnnotation(method, PostAuthorize.class);
        if (post != null) MethodSecurityExpressionEvaluator.check(post.value());

        return result;
    }

    private <A extends java.lang.annotation.Annotation> A resolveAnnotation(Method method, Class<A> type) {
        A ann = method.getAnnotation(type);
        if (ann != null) return ann;

        // Fall through to class-level annotation
        try {
            Class<?> declaringClass = method.getDeclaringClass();
            return declaringClass.getAnnotation(type);
        } catch (Exception e) {
            return null;
        }
    }
}