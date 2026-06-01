package io.lumen.async;

import io.lumen.async.annotation.Async;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.proxy.ProxyFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AsyncProcessor implements LightProcessor {

    private final ExecutorService executor;
    private final AsyncUncaughtExceptionHandler exceptionHandler;

    public AsyncProcessor(ExecutorService executor, AsyncUncaughtExceptionHandler exceptionHandler) {
        this.executor = executor;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        if (!hasAsyncMethods(instance.getClass())) return instance;

        @SuppressWarnings("unchecked")
        Class<Object> type = (Class<Object>) instance.getClass();
        return ProxyFactory.createDelegatingProxy(type, instance,
                List.of(new AsyncInterceptor(executor, exceptionHandler)));
    }

    private boolean hasAsyncMethods(Class<?> type) {
        for (Method m : type.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Async.class)) return true;
        }
        return false;
    }
}