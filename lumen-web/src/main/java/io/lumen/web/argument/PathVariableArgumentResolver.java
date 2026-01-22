package io.lumen.web.argument;

import io.lumen.web.annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.Map;

import static io.lumen.web.util.TypeConverter.convert;

/**
 * Resolver for method parameters annotated with @PathVariable.
 */
public class PathVariableArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(PathVariable.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        PathVariable annotation = parameter.getAnnotation(PathVariable.class);
        String value = pathVariables.get(annotation.value());
        return convert(value, parameter.getType());
    }
}
