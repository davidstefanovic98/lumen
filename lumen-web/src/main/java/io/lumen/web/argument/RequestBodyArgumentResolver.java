package io.lumen.web.argument;

import io.lumen.web.annotation.RequestBody;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.Map;

public class RequestBodyArgumentResolver implements MethodArgumentResolver {

    private final HttpMessageConverterRegistry converterRegistry;

    public RequestBodyArgumentResolver(HttpMessageConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        return converterRegistry.read(
                parameter.getType(),
                parameter.getParameterizedType(),
                request
        );
    }
}
