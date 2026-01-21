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
        MAPPING_ANNOTATIONS.put(GetMapping.class, new MappingInfo(
                "GET",
                a -> !((GetMapping) a).value().isEmpty() ? ((GetMapping) a).value() : "",
                a -> ((GetMapping) a).consumes(),
                a -> ((GetMapping) a).produces()
        ));

        MAPPING_ANNOTATIONS.put(PostMapping.class, new MappingInfo(
                "POST",
                a -> !((PostMapping) a).value().isEmpty() ? ((PostMapping) a).value() : "",
                a -> ((PostMapping) a).consumes(),
                a -> ((PostMapping) a).produces()
        ));

        MAPPING_ANNOTATIONS.put(PutMapping.class, new MappingInfo(
                "PUT",
                a -> !((PutMapping) a).value().isEmpty() ? ((PutMapping) a).value() : "",
                a -> ((PutMapping) a).consumes(),
                a -> ((PutMapping) a).produces()
        ));

        MAPPING_ANNOTATIONS.put(DeleteMapping.class, new MappingInfo(
                "DELETE",
                a -> !((DeleteMapping) a).value().isEmpty() ? ((DeleteMapping) a).value() : "",
                a -> ((DeleteMapping) a).consumes(),
                a -> ((DeleteMapping) a).produces()
        ));

        MAPPING_ANNOTATIONS.put(PatchMapping.class, new MappingInfo(
                "PATCH",
                a -> !((PatchMapping) a).value().isEmpty() ? ((PatchMapping) a).value() : "",
                a -> ((PatchMapping) a).consumes(),
                a -> ((PatchMapping) a).produces()
        ));
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
        boolean classIsRest = hasAnnotation(type, ResponseBody.class);

        for (Method method : type.getDeclaredMethods()) {
            for (Map.Entry<Class<? extends Annotation>, MappingInfo> entry : MAPPING_ANNOTATIONS.entrySet()) {
                if (method.isAnnotationPresent(entry.getKey())) {
                    Annotation annotation = method.getAnnotation(entry.getKey());
                    MappingInfo info = entry.getValue();

                    String methodPath = info.pathExtractor.apply(annotation);
                    String fullPath = combinePaths(basePath, methodPath);

                    Route route = new Route(instance, method, info.httpMethod, fullPath);

                    boolean methodIsRest = hasAnnotation(method, ResponseBody.class);
                    route.setRest(classIsRest || methodIsRest);

                    route.setConsumes(info.consumesExtractor.apply(annotation));
                    route.setProduces(info.producesExtractor.apply(annotation));

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

    private static class MappingInfo {
        String httpMethod;
        Function<Annotation, String> pathExtractor;
        Function<Annotation, String[]> consumesExtractor;
        Function<Annotation, String[]> producesExtractor;

        public MappingInfo(String httpMethod,
                           Function<Annotation, String> pathExtractor,
                           Function<Annotation, String[]> consumesExtractor,
                           Function<Annotation, String[]> producesExtractor) {
            this.httpMethod = httpMethod;
            this.pathExtractor = pathExtractor;
            this.consumesExtractor = consumesExtractor;
            this.producesExtractor = producesExtractor;
        }
    }
}