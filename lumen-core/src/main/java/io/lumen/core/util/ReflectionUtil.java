package io.lumen.core.util;

import io.lumen.core.exception.LightInitializationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;

public class ReflectionUtil {

    /**
     * Find the most suitable constructor for the given class.
     * If multiple constructors exist, the one with the most parameters is chosen.
     */
    public static Constructor<?> findConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();

        if (constructors.length == 0) {
            throw new LightInitializationException("No constructor found for " + type.getName());
        }

        Constructor<?> selected;

        if (constructors.length == 1) {
            selected = constructors[0];
        } else {
            // Multiple constructors - pick the one with the most parameters
            selected = null;
            int maxParams = -1;
            for (Constructor<?> ctor : constructors) {
                if (ctor.getParameterCount() > maxParams) {
                    maxParams = ctor.getParameterCount();
                    selected = ctor;
                }
            }
        }

        if (selected == null) {
            // This should never happen, but we guard against NPE
            throw new LightInitializationException(
                    "Failed to select a constructor for " + type.getName()
            );
        }

        selected.setAccessible(true);
        return selected;
    }

    /**
     * Extract generic type from a collection parameter.
     */
    public static Class<?> getGenericType(Parameter parameter) {
        java.lang.reflect.Type genericType = parameter.getParameterizedType();

        if (genericType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();

            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }

        // Fallback to Object if we can't determine the type
        return Object.class;
    }

    public static boolean hasAnnotation(Class<?> target, Class<? extends Annotation> annotation) {
        return hasAnnotation(target, annotation, new HashSet<>());
    }

    private static boolean hasAnnotation(
            AnnotatedElement element,
            Class<? extends Annotation> target,
            Set<AnnotatedElement> visited
    ) {
        if (visited.contains(element))
            return false;

        visited.add(element);

        if (element.isAnnotationPresent(target)) return true;

        for (Annotation ann : element.getAnnotations()) {
            if (hasAnnotation(ann.annotationType(), target, visited)) {
                return true;
            }
        }
        return false;
    }
}
