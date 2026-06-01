package io.lumen.async.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {
    /** Name of the executor to use. Defaults to the framework-managed pool. */
    String value() default "default";
}