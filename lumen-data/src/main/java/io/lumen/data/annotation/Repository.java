package io.lumen.data.annotation;

import io.lumen.context.annotation.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
public @interface Repository {
    String value() default "";
}