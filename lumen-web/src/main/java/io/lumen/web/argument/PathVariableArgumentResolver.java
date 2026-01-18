package io.lumen.web.argument;

import io.lumen.web.annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

import static io.lumen.web.util.TypeConverter.convert;

/**
 * Resolver for method parameters annotated with @PathVariable.
 */
class PathVariableArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(PathVariable.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, Map<String, String> pathVariables) {
        PathVariable annotation = parameter.getAnnotation(PathVariable.class);
        String value = pathVariables.get(annotation.value());
        return convert(value, parameter.getType());
    }
}
