package io.lumen.web.annotation;

import io.lumen.web.http.HttpStatus;

import java.lang.annotation.*;

/**
 * Declares the HTTP status for a handler method's response, or - on an exception class - the
 * status the framework responds with when that exception escapes a handler. {@link Inherited},
 * so a subclass of an annotated exception (e.g. a user's own {@code NotFoundException} subtype)
 * keeps its parent's status instead of silently falling back to 500.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ResponseStatus {
    HttpStatus value();
    String reason() default "";
}