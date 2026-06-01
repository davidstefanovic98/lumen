package io.lumen.async;

import io.lumen.async.annotation.Scheduled;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

import java.lang.reflect.Method;

record ScheduledTask(Object bean, Method method, Scheduled annotation) {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTask.class);

    void run() {
        try {
            method.invoke(bean);
        } catch (Exception e) {
            logger.error("@Scheduled method {}.{} threw: {}",
                    bean.getClass().getSimpleName(), method.getName(), e.getMessage(), e);
        }
    }

    String cron()       { return annotation.cron(); }
    long fixedRate()    { return annotation.fixedRate(); }
    long fixedDelay()   { return annotation.fixedDelay(); }
    long initialDelay() { return annotation.initialDelay(); }
}