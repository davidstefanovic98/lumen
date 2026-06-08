package io.lumen.async;

import io.lumen.async.annotation.Async;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.proxy.ProxyFactory;
import io.lumen.core.task.TaskDecorator;
import io.lumen.core.util.ReflectionUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class AsyncProcessor implements LightProcessor {

    private final ExecutorService executor;
    private final AsyncUncaughtExceptionHandler exceptionHandler;
    private final Supplier<List<TaskDecorator>> decorators;

    public AsyncProcessor(ExecutorService executor,
                          AsyncUncaughtExceptionHandler exceptionHandler,
                          Supplier<List<TaskDecorator>> decorators) {
        this.executor = executor;
        this.exceptionHandler = exceptionHandler;
        this.decorators = decorators;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        if (!hasAsyncMethods(instance.getClass())) return instance;

        @SuppressWarnings("unchecked")
        Class<Object> type = (Class<Object>) instance.getClass();
        return ProxyFactory.createDelegatingProxy(type, instance,
                List.of(new AsyncInterceptor(executor, exceptionHandler, decorators)));
    }

    private boolean hasAsyncMethods(Class<?> type) {
        for (Method m : type.getDeclaredMethods()) {
            if (ReflectionUtil.findAnnotation(m, Async.class) != null) return true;
        }
        return false;
    }
}