package io.lumen.web.annotation;

import io.lumen.web.http.HttpStatus;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseStatus {
    HttpStatus value();
    String reason() default "";
}