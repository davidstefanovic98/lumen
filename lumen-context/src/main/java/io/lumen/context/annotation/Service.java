package io.lumen.context.annotation;

import java.lang.annotation.*;

// Marks a class as a service to be managed by the Lumen context
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@Documented
public @interface Service {
}
