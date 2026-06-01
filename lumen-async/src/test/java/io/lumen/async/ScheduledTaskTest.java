package io.lumen.async;

import io.lumen.async.annotation.Scheduled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledTaskTest {

    ScheduledExecutorService scheduler;

    @BeforeEach void setUp()    { scheduler = Executors.newScheduledThreadPool(2); }
    @AfterEach  void tearDown() { scheduler.shutdownNow(); }

    // --- ScheduledTaskProcessor ---

    static class RateService {
        @Scheduled(fixedRate = 100) public void tick() {}
    }

    static class DelayService {
        @Scheduled(fixedDelay = 100, initialDelay = 50) public void poll() {}
    }

    static class NoAnnotationService {
        public void doWork() {}
    }

    static class InvalidService {
        @Scheduled public void broken() {}  // neither fixedRate nor fixedDelay
    }

    @Test
    void processor_collectsFixedRateTask() {
        List<ScheduledTask> tasks = new ArrayList<>();
        ScheduledTaskProcessor processor = new ScheduledTaskProcessor(tasks);
        processor.afterInstantiation(null, new RateService());
        assertEquals(1, tasks.size());
        assertEquals(100, tasks.getFirst().fixedRate());
    }

    @Test
    void processor_collectsFixedDelayTask() {
        List<ScheduledTask> tasks = new ArrayList<>();
        ScheduledTaskProcessor processor = new ScheduledTaskProcessor(tasks);
        processor.afterInstantiation(null, new DelayService());
        assertEquals(1, tasks.size());
        assertEquals(100,  tasks.getFirst().fixedDelay());
        assertEquals(50,   tasks.getFirst().initialDelay());
    }

    @Test
    void processor_skipsBeansWithNoScheduledMethods() {
        List<ScheduledTask> tasks = new ArrayList<>();
        ScheduledTaskProcessor processor = new ScheduledTaskProcessor(tasks);
        processor.afterInstantiation(null, new NoAnnotationService());
        assertTrue(tasks.isEmpty());
    }

    @Test
    void processor_throwsOnInvalidAnnotation() {
        List<ScheduledTask> tasks = new ArrayList<>();
        ScheduledTaskProcessor processor = new ScheduledTaskProcessor(tasks);
        assertThrows(IllegalArgumentException.class,
                () -> processor.afterInstantiation(null, new InvalidService()));
    }

    // --- ScheduledTaskInitializer ---

    @Test
    void initializer_startsFixedRateTask() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(3);

        // Build a ScheduledTask manually around a lambda
        Runnable work = () -> { counter.incrementAndGet(); latch.countDown(); };

        List<ScheduledTask> tasks = new ArrayList<>();
        ScheduledTask task = buildTask(work, 50, -1, 0);
        tasks.add(task);

        ScheduledTaskInitializer initializer = new ScheduledTaskInitializer(scheduler, tasks);
        initializer.onStartup();

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Task should have fired at least 3 times within 1s");
        assertTrue(counter.get() >= 3);
    }

    @Test
    void initializer_respectsInitialDelay() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        List<ScheduledTask> tasks = new ArrayList<>();
        tasks.add(buildTask(counter::incrementAndGet, 50, -1, 300));

        ScheduledTaskInitializer initializer = new ScheduledTaskInitializer(scheduler, tasks);
        initializer.onStartup();

        Thread.sleep(100);
        assertEquals(0, counter.get(), "Task should not have fired yet during initialDelay");

        Thread.sleep(400);
        assertTrue(counter.get() > 0, "Task should have fired after initialDelay elapsed");
    }

    // --- Helper to create a ScheduledTask around an arbitrary Runnable ---

    private ScheduledTask buildTask(Runnable work, long fixedRate, long fixedDelay, long initialDelay) {
        try {
            // Use a real @Scheduled method to get a valid annotation instance
            var method = ScheduledTaskTest.class.getDeclaredMethod("placeholder");
            method.setAccessible(true);
            Object bean = new Object() {
                public void run() { work.run(); }
            };
            var runMethod = bean.getClass().getMethod("run");
            Scheduled ann = new Scheduled() {
                public Class<Scheduled> annotationType() { return Scheduled.class; }
                public String cron()       { return "";           }
                public long fixedRate()    { return fixedRate;    }
                public long fixedDelay()   { return fixedDelay;   }
                public long initialDelay() { return initialDelay; }
            };
            return new ScheduledTask(bean, runMethod, ann);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedRate = 1000)
    private void placeholder() {}
}