package io.lumen.core.util;

import io.lumen.core.exception.LightInitializationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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
}
