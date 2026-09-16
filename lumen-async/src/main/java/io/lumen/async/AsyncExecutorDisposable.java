package io.lumen.async;

import io.lumen.core.LumenDisposable;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class AsyncExecutorDisposable implements LumenDisposable {

    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutorDisposable.class);

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;

    AsyncExecutorDisposable(ExecutorService executor, ScheduledExecutorService scheduler) {
        this.executor = executor;
        this.scheduler = scheduler;
    }

    @Override
    public void onShutdown() {
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
    }
}