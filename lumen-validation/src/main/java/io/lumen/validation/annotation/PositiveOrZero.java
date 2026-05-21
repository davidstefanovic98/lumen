package io.lumen.validation.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PositiveOrZero {
    String message() default "must be greater than or equal to 0";
}