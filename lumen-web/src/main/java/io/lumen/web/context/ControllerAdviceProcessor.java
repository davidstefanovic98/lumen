package io.lumen.web.context;

import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.web.annotation.ControllerAdvice;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;

import static io.lumen.core.util.ReflectionUtil.hasAnnotation;

public class ControllerAdviceProcessor implements LightProcessor {

    private final ControllerAdviceRegistry adviceRegistry;

    public ControllerAdviceProcessor(ControllerAdviceRegistry adviceRegistry) {
        this.adviceRegistry = adviceRegistry;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        Class<?> type = instance.getClass();

        if (hasAnnotation(type, ControllerAdvice.class)) {
            GlobalExceptionHandlerScanner.scanInstance(instance, type, adviceRegistry);
        }

        return instance;
    }
}
