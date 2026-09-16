package io.lumen.web.argument;

import io.lumen.core.util.ParameterNameDiscoverer;
import io.lumen.web.annotation.PathVariable;
import io.lumen.web.exception.PathVariableNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.Map;

import static io.lumen.web.util.TypeConverter.convert;

public class PathVariableArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(PathVariable.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        PathVariable annotation = parameter.getAnnotation(PathVariable.class);
        String name = annotation.value().isEmpty()
                ? ParameterNameDiscoverer.getParameterName(parameter)
                : annotation.value();

        if (!pathVariables.containsKey(name)) {
            throw new PathVariableNotFoundException(String.format(
                    "No path variable '%s' found for method parameter of type %s. " +
                            "Check that the route pattern declares a matching '{%s}' segment.",
                    name, parameter.getType().getSimpleName(), name
            ));
        }
        return convert(pathVariables.get(name), parameter.getType());
    }
}
