package io.lumen.async;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AsyncExecutorDisposableTest {

    @Test
    void onShutdown_shutsDownBothExecutorAndScheduler() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        new AsyncExecutorDisposable(executor, scheduler).onShutdown();

        assertTrue(executor.isShutdown());
        assertTrue(scheduler.isShutdown());
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }
}