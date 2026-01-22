package io.lumen.web.argument;

import io.lumen.web.annotation.ModelAttribute;
import io.lumen.web.util.ObjectBinder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.Map;

import static io.lumen.web.util.TypeInspection.isBindableType;

/**
 * Resolver for method arguments annotated with @ModelAttribute
 * or without annotation but of complex type.
 */
public class ModelAttributeArgumentResolver implements MethodArgumentResolver{

    @Override
    public boolean supports(Parameter parameter) {
        return isBindableType(parameter.getType()) || parameter.isAnnotationPresent(ModelAttribute.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        return ObjectBinder.bind(parameter.getType(), request);
    }
}
