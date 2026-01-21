package io.lumen.web.util;

import io.lumen.core.util.ParameterNameDiscoverer;
import io.lumen.core.util.ReflectionUtil;
import io.lumen.web.exception.BindRequestParamException;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.lumen.web.util.TypeConverter.convert;

/**
 * Utility class for binding HTTP request parameters to Java objects.
 * <p>
 * Supports both:
 * 1. Immutable objects with constructor binding (requires -parameters compiler flag or @ConstructorBinding).
 * 2. Mutable objects via no-args constructor + field binding.
 * <p>
 * Optimization:
 * - Fields of each class are cached after first access in a ConcurrentHashMap.
 * - Each Field is set accessible once when cached, avoiding repeated reflection overhead.
 * <p>
 */
public final class ObjectBinder {

    private ObjectBinder() {}

    /**
     * Cache for class fields. Key: class type, Value: array of declared fields (set accessible).
     * This ensures we only compute fields and call setAccessible(true) once per class,
     * improving performance for repeated bindings of the same type.
     */
    private static final Map<Class<?>, Field[]> classFieldCache = new ConcurrentHashMap<>();

    /**
     * Bind request parameters to an instance of the specified type.
     * <p>
     * 1. Attempts constructor binding first (for immutable objects).
     *    - Uses constructor parameter names (requires -parameters or @ConstructorBinding).
     *    - Skips if parameter names are unavailable (synthetic like arg0, arg1).
     * 2. Falls back to no-args constructor + field binding (for mutable objects).
     *    - Uses cached Field[] from classFieldCache.
     *    - Field accessibility is set once at caching time.
     *
     * @param type    the class to bind to
     * @param request the HTTP request containing parameters
     * @return a populated instance of the specified type
     * @throws BindRequestParamException if binding fails or no suitable constructor is available
     */
    public static Object bind(Class<?> type, HttpServletRequest request) {
        try {
            Constructor<?> ctor = ReflectionUtil.findConstructor(type);
            Parameter[] ctorParams = ctor.getParameters();

            if (ctorParams.length > 0) {
                String[] paramNames = ParameterNameDiscoverer.getParameterNames(ctor);
                Map<String, String> missing = new LinkedHashMap<>();
                Object[] args = new Object[ctorParams.length];

                for (int i = 0; i < ctorParams.length; i++) {
                    String name = paramNames[i];
                    String value = request.getParameter(name);
                    if (value == null) {
                        missing.put(name, ctorParams[i].getType().getSimpleName());
                    } else {
                        args[i] = convert(value, ctorParams[i].getType());
                    }
                }

                if (missing.isEmpty()) {
                    return ctor.newInstance(args);
                }
            }

            Object instance;
            try {
                instance = type.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                throw new BindRequestParamException(
                        "Cannot bind type " + type.getName() +
                                ": no no-args constructor and constructor parameter names are unavailable. " +
                                "For immutable types, compile with -parameters or introduce @ConstructorBinding.",
                        e
                );
            }

            Field[] fields = classFieldCache.computeIfAbsent(type, clazz -> {
                Field[] allFields = clazz.getDeclaredFields();
                for (Field f : allFields)
                    f.setAccessible(true);
                return allFields;
            });

            for (Field field : fields) {
                String value = request.getParameter(field.getName());
                if (value != null) {
                    field.set(instance, convert(value, field.getType()));
                }
            }

            return instance;

        } catch (Exception e) {
            throw new BindRequestParamException(
                    "Failed to bind request parameters to type " + type.getName() +
                            ". Available parameters: " + request.getParameterMap().keySet(),
                    e
            );
        }
    }
}
