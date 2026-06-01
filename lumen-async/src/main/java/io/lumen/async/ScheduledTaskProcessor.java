package io.lumen.async;

import io.lumen.async.annotation.Scheduled;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;

import java.lang.reflect.Method;
import java.util.List;

public class ScheduledTaskProcessor implements LightProcessor {

    private final List<ScheduledTask> tasks;

    public ScheduledTaskProcessor(List<ScheduledTask> tasks) {
        this.tasks = tasks;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        for (Method method : instance.getClass().getDeclaredMethods()) {
            Scheduled ann = method.getAnnotation(Scheduled.class);
            if (ann == null) continue;
            boolean hasCron  = !ann.cron().isBlank();
            boolean hasFixed = ann.fixedRate() >= 0 || ann.fixedDelay() >= 0;
            if (!hasCron && !hasFixed) {
                throw new IllegalArgumentException(
                        "@Scheduled on " + instance.getClass().getSimpleName() + "#" + method.getName()
                                + " must declare cron, fixedRate, or fixedDelay");
            }
            if (hasCron && hasFixed) {
                throw new IllegalArgumentException(
                        "@Scheduled on " + instance.getClass().getSimpleName() + "#" + method.getName()
                                + ": cron is mutually exclusive with fixedRate/fixedDelay");
            }
            method.setAccessible(true);
            tasks.add(new ScheduledTask(instance, method, ann));
        }
        return instance;
    }
}