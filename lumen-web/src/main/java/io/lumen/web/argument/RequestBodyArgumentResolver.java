package io.lumen.web.argument;

import io.lumen.web.annotation.RequestBody;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

class RequestBodyArgumentResolver implements MethodArgumentResolver {

    private final HttpMessageConverterRegistry converterRegistry;

    RequestBodyArgumentResolver(HttpMessageConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, Map<String, String> pathVariables) {
        return converterRegistry.read(
                parameter.getType(),
                parameter.getParameterizedType(),
                request
        );
    }
}
