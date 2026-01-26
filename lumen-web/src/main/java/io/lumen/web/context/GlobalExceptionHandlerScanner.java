package io.lumen.web.context;

import io.lumen.web.annotation.ControllerAdvice;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;

import static io.lumen.core.util.ReflectionUtil.hasAnnotation;

class GlobalExceptionHandlerScanner {
    private GlobalExceptionHandlerScanner() {}

    static void scanInstance(Object instance, Class<?> type, ControllerAdviceRegistry registry) {
        if (hasAnnotation(type, ControllerAdvice.class)) {
            registry.registerAdvice(instance);
        }
    }
}
