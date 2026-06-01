package io.lumen.async.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {
    /**
     * 6-field cron expression: {@code <second> <minute> <hour> <dom> <month> <dow>}.
     * Mutually exclusive with {@link #fixedRate()} and {@link #fixedDelay()}.
     * Example: {@code "0 * * * * *"} = every minute at second 0.
     */
    String cron() default "";

    /** Run every N milliseconds regardless of how long the previous run took. */
    long fixedRate() default -1;

    /** Wait N milliseconds after the previous run finishes before starting the next. */
    long fixedDelay() default -1;

    /** How long to wait before the first execution (ms). Ignored for cron. */
    long initialDelay() default 0;
}