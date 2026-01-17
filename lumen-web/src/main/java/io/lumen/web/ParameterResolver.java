package io.lumen.web;

import io.lumen.core.util.ReflectionUtil;
import io.lumen.web.annotation.PathVariable;
import io.lumen.web.annotation.RequestParam;
import io.lumen.web.exception.BindRequestParamException;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves controller method parameters from HTTP requests.
 * Supports:
 * - @PathVariable
 * - @RequestParam
 * - Object binding (mutable or immutable)
 */
class ParameterResolver {

    Object[] resolveParameters(Parameter[] parameters, HttpServletRequest request, Map<String, String> pathVariables) {
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolveParameter(parameters[i], request, pathVariables);
        }

        return args;
    }

    private Object resolveParameter(Parameter param, HttpServletRequest request, Map<String, String> pathVariables) {
        Class<?> type = param.getType();

        if (param.isAnnotationPresent(PathVariable.class)) {
            return resolvePathVariable(param, pathVariables);
        }

        if (param.isAnnotationPresent(RequestParam.class)) {
            RequestParam rp = param.getAnnotation(RequestParam.class);
            if (isSimpleType(type)) {
                String value = request.getParameter(rp.value());
                if (value == null && !rp.defaultValue().isEmpty()) {
                    value = rp.defaultValue();
                }
                return convertValue(value, type);
            } else {
                return bindObject(type, request);
            }
        }

        if (isBindableType(type)) {
            return bindObject(type, request);
        }

        return null;
    }

    private Object resolvePathVariable(Parameter param, Map<String, String> pathVariables) {
        String name = param.getAnnotation(PathVariable.class).value();
        String value = pathVariables.get(name);
        return convertValue(value, param.getType());
    }

    /**
     * Bind request parameters to an object of the given type.
     * First we try to bind using the constructor (for immutable objects).
     * If that fails, we fall back to no-args constructor + field binding (for mutable objects).
     * @param type the class type to bind to
     * @param request the HTTP request
     * @return the bound object
     */
    private Object bindObject(Class<?> type, HttpServletRequest request) {
        try {
            Constructor<?> ctor = ReflectionUtil.findConstructor(type);
            Parameter[] ctorParams = ctor.getParameters();

            boolean hasUnknownNames = false;
            for (Parameter p : ctorParams) {
                if (p.getName().startsWith("arg")) {
                    hasUnknownNames = true;
                    break;
                }
            }

            if (!hasUnknownNames) {
                Map<String, String> missingParams = new LinkedHashMap<>();
                Object[] ctorArgs = new Object[ctorParams.length];

                for (int i = 0; i < ctorParams.length; i++) {
                    String paramName = ctorParams[i].getName();
                    String value = request.getParameter(paramName);
                    if (value == null) {
                        missingParams.put(paramName, ctorParams[i].getType().getSimpleName());
                    } else {
                        ctorArgs[i] = convertValue(value, ctorParams[i].getType());
                    }
                }

                if (missingParams.isEmpty() && ctorParams.length > 0) {
                    return ctor.newInstance(ctorArgs);
                }
            }

            // Fall back to no-args + field binding (mutable objects)
            Object instance;
            try {
                instance = type.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException nsme) {
                String msg = "Cannot bind object of type " + type.getName() +
                        ": no-args constructor is missing and constructor binding is unavailable." +
                        " For immutable classes, enable -parameters or annotate the constructor with @ConstructorBinding.";
                throw new BindRequestParamException(msg, nsme);
            }

            for (Field field : type.getDeclaredFields()) {
                String value = request.getParameter(field.getName());
                if (value != null) {
                    field.setAccessible(true);
                    field.set(instance, convertValue(value, field.getType()));
                }
            }

            return instance;
        } catch (Exception e) {
            String msg = "Failed to bind request parameters to " + type.getName() +
                    ". Available request parameters: " + request.getParameterMap().keySet() +
                    ". Ensure constructor parameters or field names match the request parameters." +
                    " For immutable classes, enable -parameters or annotate the constructor with @ConstructorBinding.";
            throw new BindRequestParamException(msg, e);
        }
    }


    /** Check if class has a no-args constructor */
    private boolean hasNoArgsConstructor(Class<?> type) {
        try {
            type.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Double.class
                || type == Long.class
                || type == Integer.class
                || type == Short.class
                || type == Byte.class
                || type == Float.class
                || type == Character.class;
    }

    private boolean isBindableType(Class<?> type) {
        return !isSimpleType(type) && !type.getName().startsWith("java.") && !type.getName().startsWith("jakarta.");
    }

    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        throw new IllegalArgumentException("Unsupported parameter type: " + targetType);
    }
}
