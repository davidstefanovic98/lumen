package io.lumen.web.context;

import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.web.RouteRegistry;
import io.lumen.web.annotation.Controller;

import static io.lumen.core.util.ReflectionUtil.hasAnnotation;

public class ControllerProcessor implements LightProcessor {

    private final RouteRegistry routeRegistry;

    public ControllerProcessor(RouteRegistry routeRegistry) {
        this.routeRegistry = routeRegistry;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        Class<?> type = instance.getClass();

        if (hasAnnotation(type, Controller.class)) {
            ControllerScanner.scanController(instance, type, routeRegistry);
        }

        return instance;
    }
}
