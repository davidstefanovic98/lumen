package io.lumen.async;

import io.lumen.async.annotation.Async;
import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class AsyncInterceptor implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncInterceptor.class);

    private final ExecutorService executor;
    private final AsyncUncaughtExceptionHandler exceptionHandler;

    public AsyncInterceptor(ExecutorService executor, AsyncUncaughtExceptionHandler exceptionHandler) {
        this.executor = executor;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        if (!method.isAnnotationPresent(Async.class)) {
            return invocation.proceed();
        }

        boolean returnsFuture = CompletableFuture.class.isAssignableFrom(method.getReturnType());
        Object[] args = invocation.getArguments();

        if (returnsFuture) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            executor.submit(() -> {
                try {
                    Object result = invocation.proceed();
                    // If the method itself returns a CompletableFuture, unwrap it so the
                    // caller gets CompletableFuture<T> not CompletableFuture<CompletableFuture<T>>.
                    if (result instanceof CompletableFuture<?> inner) {
                        inner.whenComplete((val, ex) -> {
                            if (ex != null) future.completeExceptionally(ex);
                            else            future.complete(val);
                        });
                    } else {
                        future.complete(result);
                    }
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        }

        executor.submit(() -> {
            try {
                invocation.proceed();
            } catch (Throwable t) {
                exceptionHandler.handleUncaughtException(t, method, args);
            }
        });
        return null;
    }
}