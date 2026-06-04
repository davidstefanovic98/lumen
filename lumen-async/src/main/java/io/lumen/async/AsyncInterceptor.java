package io.lumen.async;

import io.lumen.async.annotation.Async;
import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.task.TaskDecorator;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class AsyncInterceptor implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncInterceptor.class);

    private final ExecutorService executor;
    private final AsyncUncaughtExceptionHandler exceptionHandler;
    private final Supplier<List<TaskDecorator>> decorators;

    public AsyncInterceptor(ExecutorService executor,
                            AsyncUncaughtExceptionHandler exceptionHandler,
                            Supplier<List<TaskDecorator>> decorators) {
        this.executor = executor;
        this.exceptionHandler = exceptionHandler;
        this.decorators = decorators;
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
            Runnable task = () -> {
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
            };
            executor.submit(decorate(task));
            return future;
        }

        Runnable task = () -> {
            try {
                invocation.proceed();
            } catch (Throwable t) {
                exceptionHandler.handleUncaughtException(t, method, args);
            }
        };
        executor.submit(decorate(task));
        return null;
    }

    /**
     * Applies all registered {@link TaskDecorator}s to the task. Invoked on the calling
     * thread so each decorator can snapshot thread-bound context before submission.
     */
    private Runnable decorate(Runnable task) {
        Runnable decorated = task;
        for (TaskDecorator decorator : decorators.get()) {
            decorated = decorator.decorate(decorated);
        }
        return decorated;
    }
}