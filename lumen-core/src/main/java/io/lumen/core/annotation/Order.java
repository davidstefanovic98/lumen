package io.lumen.core.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Order {
    /**
     * Lower values have higher priority. Default is max value (lowest priority).
     */
    int value() default Integer.MAX_VALUE;
}
