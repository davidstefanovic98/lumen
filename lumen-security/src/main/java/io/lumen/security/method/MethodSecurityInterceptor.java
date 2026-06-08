package io.lumen.security.method;

import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.core.util.ReflectionUtil;
import io.lumen.security.annotation.PostAuthorize;
import io.lumen.security.annotation.PreAuthorize;

import java.lang.reflect.Method;

public class MethodSecurityInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object[] args = invocation.getArguments();

        PreAuthorize pre = ReflectionUtil.findAnnotation(method, PreAuthorize.class);
        if (pre != null) MethodSecurityExpressionEvaluator.checkPre(pre.value(), method, args);

        Object result = invocation.proceed();

        PostAuthorize post = ReflectionUtil.findAnnotation(method, PostAuthorize.class);
        if (post != null) MethodSecurityExpressionEvaluator.checkPost(post.value(), method, args, result);

        return result;
    }
}