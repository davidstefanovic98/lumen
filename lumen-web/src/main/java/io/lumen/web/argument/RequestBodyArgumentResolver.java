package io.lumen.web.argument;

import io.lumen.web.annotation.RequestBody;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

public class RequestBodyArgumentResolver implements MethodArgumentResolver {

    private static final String VALID_ANNOTATION   = "io.lumen.validation.annotation.Valid";
    private static final String VALIDATOR_INTERFACE = "io.lumen.validation.Validator";

    private final HttpMessageConverterRegistry converterRegistry;
    // Held as Object so this class loads cleanly when lumen-validation is absent.
    private Object validator;

    public RequestBodyArgumentResolver(HttpMessageConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    public void setValidator(Object validator) {
        this.validator = validator;
    }

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        Object body = converterRegistry.read(
                parameter.getType(),
                parameter.getParameterizedType(),
                request
        );

        if (body != null && validator != null && hasValidAnnotation(parameter)) {
            validateAndThrow(body);
        }

        return body;
    }

    private boolean hasValidAnnotation(Parameter parameter) {
        for (Annotation ann : parameter.getAnnotations()) {
            if (ann.annotationType().getName().equals(VALID_ANNOTATION)) return true;
        }
        return false;
    }

    private void validateAndThrow(Object body) {
        try {
            Class<?> validatorClass = Class.forName(VALIDATOR_INTERFACE);
            Method method = validatorClass.getMethod("validateAndThrow", Object.class);
            method.invoke(validator, body);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException("Validation invocation failed", e);
        }
    }
}