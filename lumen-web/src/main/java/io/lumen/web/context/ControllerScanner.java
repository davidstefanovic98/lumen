package io.lumen.web.context;

import io.lumen.context.annotation.Controller;
import io.lumen.core.component.LightInstance;
import io.lumen.web.Route;
import io.lumen.web.RouteRegistry;
import io.lumen.web.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class ControllerScanner {

    private static final Map<Class<? extends Annotation>, MappingInfo> MAPPING_ANNOTATIONS = new HashMap<>();

    static {
        MAPPING_ANNOTATIONS.put(GetMapping.class, new MappingInfo("GET", a -> ((GetMapping) a).value()));
        MAPPING_ANNOTATIONS.put(PostMapping.class, new MappingInfo("POST", a -> ((PostMapping) a).value()));
        MAPPING_ANNOTATIONS.put(PutMapping.class, new MappingInfo("PUT", a -> ((PutMapping) a).value()));
        MAPPING_ANNOTATIONS.put(DeleteMapping.class, new MappingInfo("DELETE", a -> ((DeleteMapping) a).value()));
        MAPPING_ANNOTATIONS.put(PatchMapping.class, new MappingInfo("PATCH", a -> ((PatchMapping) a).value()));
    }

    static void scanControllers(Collection<LightInstance> lights, RouteRegistry registry) {
        for (LightInstance light : lights) {
            if (light.getType().isAnnotationPresent(Controller.class)) {
                scanController(light.getInstance(), light.getType(), registry);
            }
        }
    }

    private static void scanController(Object instance, Class<?> type, RouteRegistry registry) {
        for (Method method : type.getDeclaredMethods()) {
            for (Map.Entry<Class<? extends Annotation>, MappingInfo> entry : MAPPING_ANNOTATIONS.entrySet()) {
                if (method.isAnnotationPresent(entry.getKey())) {
                    Annotation annotation = method.getAnnotation(entry.getKey());
                    MappingInfo info = entry.getValue();

                    String path = info.pathExtractor.apply(annotation);
                    Route route = new Route(instance, method, info.httpMethod, path);
                    registry.register(route);
                }
            }
        }
    }

    private record MappingInfo(String httpMethod, Function<Annotation, String> pathExtractor) {}
}