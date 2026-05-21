package io.lumen.web.argument;

import io.lumen.core.util.ParameterNameDiscoverer;
import io.lumen.web.annotation.PathVariable;
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
        return convert(pathVariables.get(name), parameter.getType());
    }
}
