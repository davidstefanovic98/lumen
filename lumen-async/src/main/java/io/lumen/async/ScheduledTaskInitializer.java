package io.lumen.async;

import io.lumen.async.cron.CronExpression;
import io.lumen.core.LumenInitializer;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledTaskInitializer implements LumenInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskInitializer.class);

    private final ScheduledExecutorService scheduler;
    private final List<ScheduledTask> tasks;

    public ScheduledTaskInitializer(ScheduledExecutorService scheduler, List<ScheduledTask> tasks) {
        this.scheduler = scheduler;
        this.tasks = tasks;
    }

    @Override
    public void onStartup() {
        for (ScheduledTask task : tasks) {
            String name = task.bean().getClass().getSimpleName() + "#" + task.method().getName();
            if (!task.cron().isBlank()) {
                scheduleCron(task, CronExpression.parse(task.cron()), name);
            } else if (task.fixedRate() >= 0) {
                scheduler.scheduleAtFixedRate(task::run,
                        task.initialDelay(), task.fixedRate(), TimeUnit.MILLISECONDS);
                logger.info("Scheduled [{}] fixedRate={}ms initialDelay={}ms",
                        name, task.fixedRate(), task.initialDelay());
            } else {
                scheduler.scheduleWithFixedDelay(task::run,
                        task.initialDelay(), task.fixedDelay(), TimeUnit.MILLISECONDS);
                logger.info("Scheduled [{}] fixedDelay={}ms initialDelay={}ms",
                        name, task.fixedDelay(), task.initialDelay());
            }
        }
    }

    private void scheduleCron(ScheduledTask task, CronExpression cron, String name) {
        Runnable wrapper = new Runnable() {
            @Override
            public void run() {
                task.run();
                scheduleNext(this, cron, name);
            }
        };
        logger.info("Scheduled [{}] cron=\"{}\"", name, cron);
        scheduleNext(wrapper, cron, name);
    }

    private void scheduleNext(Runnable runnable, CronExpression cron, String name) {
        ZonedDateTime now  = ZonedDateTime.now();
        ZonedDateTime next = cron.nextExecution(now);
        long delayMs = Duration.between(now, next).toMillis();
        scheduler.schedule(runnable, delayMs, TimeUnit.MILLISECONDS);
        logger.debug("Scheduled [{}] next execution at {}", name, next);
    }
}