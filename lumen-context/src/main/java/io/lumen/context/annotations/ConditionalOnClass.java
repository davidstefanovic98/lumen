package io.lumen.context.annotations;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalOnClass {
    Class<?>[] value() default {};
    String[] name() default {};
}
