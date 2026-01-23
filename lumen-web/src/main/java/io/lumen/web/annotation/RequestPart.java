package io.lumen.web.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestPart {
    String value() default "";
    boolean required() default true;
}
