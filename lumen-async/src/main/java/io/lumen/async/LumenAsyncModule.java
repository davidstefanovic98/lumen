package io.lumen.async;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.task.TaskDecorator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Order(3)
public class LumenAsyncModule implements LumenModule {

    private static final Logger logger = LoggerFactory.getLogger(LumenAsyncModule.class);

    private static final AsyncUncaughtExceptionHandler DEFAULT_HANDLER = (ex, method, args) ->
            LoggerFactory.getLogger(AsyncInterceptor.class)
                    .error("Uncaught exception in @Async {}.{}: {}",
                            method.getDeclaringClass().getSimpleName(), method.getName(), ex.getMessage(), ex);

    @Override
    public void init(LightContainer container, String... basePackages) {
        Environment env = container.getLight(Environment.class);

        int coreThreads = intProp(env, "lumen.async.core-threads", 4);
        int schedulerThreads = intProp(env, "lumen.async.scheduler-threads", 2);

        AtomicInteger asyncCounter     = new AtomicInteger(1);
        AtomicInteger schedulerCounter = new AtomicInteger(1);

        ExecutorService executor = Executors.newFixedThreadPool(coreThreads,
                r -> { Thread t = new Thread(r, "lumen-async-" + asyncCounter.getAndIncrement());
                       t.setDaemon(true); return t; });

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(schedulerThreads,
                r -> { Thread t = new Thread(r, "lumen-scheduler-" + schedulerCounter.getAndIncrement());
                       t.setDaemon(true); return t; });

        // Allow apps to provide a custom uncaught exception handler bean
        AsyncUncaughtExceptionHandler handler = container.hasLight(AsyncUncaughtExceptionHandler.class)
                ? container.getLight(AsyncUncaughtExceptionHandler.class)
                : DEFAULT_HANDLER;

        List<ScheduledTask> scheduledTasks = new ArrayList<>();

        container.registerExternalInstance(ExecutorService.class, executor);
        container.registerExternalInstance(ScheduledExecutorService.class, scheduler);

        // Resolve TaskDecorators lazily and once: at @Async invocation time the container is
        // fully initialized, so user @Component decorators and module-provided ones (e.g. the
        // security context decorator) are all visible. Resolved on first use, then cached.
        Supplier<List<TaskDecorator>> decorators = memoize(() -> container.getLights(TaskDecorator.class));

        container.addPostProcessor(new AsyncProcessor(executor, handler, decorators));
        container.addPostProcessor(new ScheduledTaskProcessor(scheduledTasks));

        container.registerExternalInstance(ScheduledTaskInitializer.class,
                new ScheduledTaskInitializer(scheduler, scheduledTasks));

        registerShutdownHook(executor, scheduler);
    }

    private void registerShutdownHook(ExecutorService executor, ScheduledExecutorService scheduler) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Async executor shut down.");
        }, "lumen-async-shutdown"));
    }

    private int intProp(Environment env, String key, int defaultValue) {
        if (env == null) return defaultValue;
        return Integer.parseInt(env.getProperty(key, String.valueOf(defaultValue)));
    }

    /** Wraps a supplier so the delegate runs at most once; the result is cached thereafter. */
    private static <T> Supplier<T> memoize(Supplier<T> delegate) {
        return new Supplier<>() {
            private volatile T value;
            private volatile boolean resolved;

            @Override
            public T get() {
                if (!resolved) {
                    synchronized (this) {
                        if (!resolved) {
                            value = delegate.get();
                            resolved = true;
                        }
                    }
                }
                return value;
            }
        };
    }
}