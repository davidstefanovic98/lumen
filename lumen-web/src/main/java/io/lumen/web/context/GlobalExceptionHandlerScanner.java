package io.lumen.web.context;

import io.lumen.core.component.LightInstance;
import io.lumen.web.annotation.ControllerAdvice;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;

import java.util.Collection;

import static io.lumen.core.util.ReflectionUtil.hasAnnotation;

class GlobalExceptionHandlerScanner {
    private GlobalExceptionHandlerScanner() {}

    static void scanForControllerAdvice(Collection<LightInstance> lights, ControllerAdviceRegistry registry) {
        for (LightInstance light : lights) {
            if (hasAnnotation(light.getType(), ControllerAdvice.class)) {
                registry.registerAdvice(light.getInstance());
            }
        }
    }
}
