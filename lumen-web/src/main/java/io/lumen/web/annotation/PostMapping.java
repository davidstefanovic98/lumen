package io.lumen.web.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping
@Documented
public @interface PostMapping {
    String value();
    String path() default "";
    String[] consumes() default {};
    String[] produces() default {};
}
