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

import static io.lumen.core.util.ReflectionUtil.hasAnnotation;

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
            if (hasAnnotation(light.getType(), Controller.class)) {
                scanController(light.getInstance(), light.getType(), registry);
            }
        }
    }

    private static void scanController(Object instance, Class<?> type, RouteRegistry registry) {
        String basePath = extractBasePath(type);
        boolean classIsRest = type.isAnnotationPresent(ResponseBody.class);

        for (Method method : type.getDeclaredMethods()) {
            for (Map.Entry<Class<? extends Annotation>, MappingInfo> entry : MAPPING_ANNOTATIONS.entrySet()) {
                if (method.isAnnotationPresent(entry.getKey())) {
                    Annotation annotation = method.getAnnotation(entry.getKey());
                    MappingInfo info = entry.getValue();

                    String methodPath = info.pathExtractor.apply(annotation);
                    String fullPath = combinePaths(basePath, methodPath);

                    Route route = new Route(instance, method, info.httpMethod, fullPath);

                    boolean methodIsRest = method.isAnnotationPresent(ResponseBody.class);
                    route.setRest(classIsRest || methodIsRest);

                    registry.register(route);
                }
            }
        }
    }

    private static String extractBasePath(Class<?> type) {
        if (!type.isAnnotationPresent(RequestMapping.class)) {
            return "";
        }

        RequestMapping mapping = type.getAnnotation(RequestMapping.class);
        String path = mapping.path().isEmpty() ? mapping.value() : mapping.path();

        if (path.isEmpty()) {
            return "";
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path;
    }

    private static String combinePaths(String basePath, String methodPath) {
        if (basePath.isEmpty()) {
            return methodPath;
        }

        if (methodPath.isEmpty()) {
            return basePath;
        }

        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        if (!methodPath.startsWith("/")) {
            methodPath = "/" + methodPath;
        }

        return basePath + methodPath;
    }

    private record MappingInfo(String httpMethod, Function<Annotation, String> pathExtractor) {}
}