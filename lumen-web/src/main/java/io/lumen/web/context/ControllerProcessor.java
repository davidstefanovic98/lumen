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
        Class<?> controllerType = findControllerType(instance.getClass());
            if (controllerType != null) {
            ControllerScanner.scanController(instance, controllerType, routeRegistry);
        }
        return instance;
    }

    // Walk up the class hierarchy to find the actual @Controller class.
    // This handles the case where the instance is a proxy (e.g. for @PreAuthorize)
    // whose generated methods don't carry the original mapping annotations.
    private Class<?> findControllerType(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            if (hasAnnotation(current, Controller.class)) return current;
            current = current.getSuperclass();
        }
        return null;
    }
}
