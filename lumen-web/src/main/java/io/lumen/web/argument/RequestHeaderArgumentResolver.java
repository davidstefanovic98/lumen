package io.lumen.web.argument;

import io.lumen.core.util.ParameterNameDiscoverer;
import io.lumen.web.annotation.RequestHeader;
import io.lumen.web.exception.MissingRequestHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.Map;

import static io.lumen.web.util.TypeConverter.convert;

public class RequestHeaderArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestHeader.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request,
                          HttpServletResponse response, Map<String, String> pathVariables) {
        RequestHeader ann = parameter.getAnnotation(RequestHeader.class);
        String name = ann.value().isEmpty()
                ? ParameterNameDiscoverer.getParameterName(parameter)
                : ann.value();

        String value = request.getHeader(name);

        if (value == null && !ann.defaultValue().isEmpty()) {
            value = ann.defaultValue();
        }

        if (value == null && ann.required()) {
            throw new MissingRequestHeaderException(
                    "Missing required header '" + name + "' for parameter of type "
                            + parameter.getType().getSimpleName());
        }

        return convert(value, parameter.getType());
    }
}