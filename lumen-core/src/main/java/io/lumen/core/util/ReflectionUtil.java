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

    /**
     * Finds an annotation on a method by walking up the class hierarchy.
     * ByteBuddy-generated proxy override methods carry no annotations, so a plain
     * {@code method.getAnnotation()} call will miss annotations declared on the
     * original class. This method resolves through superclasses until it finds the
     * annotation at method level, then falls back to class level at each step.
     */
    public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        Class<?> current = method.getDeclaringClass();
        while (current != null && current != Object.class) {
            try {
                Method declared = current.getDeclaredMethod(method.getName(), method.getParameterTypes());
                A ann = declared.getAnnotation(annotationType);
                if (ann != null) return ann;
            } catch (NoSuchMethodException ignored) {}
            A classAnn = current.getAnnotation(annotationType);
            if (classAnn != null) return classAnn;
            current = current.getSuperclass();
        }
        return null;
    }

    public static boolean hasAnnotation(Method method, Class<? extends Annotation> annotation) {
        return hasAnnotation(method, annotation, new HashSet<>());
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

    // ── Classloader-resilient annotation helpers ──────────────────────────────
    // exec:java and similar runners can load the same annotation class via two
    // different ClassLoaders, making isAnnotationPresent() return false even
    // when the annotation IS present. These methods compare by class name instead.

    public static boolean hasAnnotationByName(AnnotatedElement element, String annotationClassName) {
        for (Annotation ann : element.getAnnotations()) {
            if (annotationClassName.equals(ann.annotationType().getName())) return true;
        }
        return false;
    }

    public static String getAnnotationStringValue(AnnotatedElement element, String annotationClassName) {
        for (Annotation ann : element.getAnnotations()) {
            if (annotationClassName.equals(ann.annotationType().getName())) {
                try {
                    return (String) ann.annotationType().getMethod("value").invoke(ann);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static boolean hasParameterAnnotationByName(Parameter parameter, String annotationClassName) {
        for (Annotation ann : parameter.getAnnotations()) {
            if (annotationClassName.equals(ann.annotationType().getName())) return true;
        }
        return false;
    }

    public static String getParameterAnnotationStringValue(Parameter parameter, String annotationClassName) {
        for (Annotation ann : parameter.getAnnotations()) {
            if (annotationClassName.equals(ann.annotationType().getName())) {
                try {
                    return (String) ann.annotationType().getMethod("value").invoke(ann);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
