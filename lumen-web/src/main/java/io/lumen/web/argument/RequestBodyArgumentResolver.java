package io.lumen.web.argument;

import io.lumen.web.annotation.RequestBody;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

public class RequestBodyArgumentResolver implements MethodArgumentResolver {

    private final HttpMessageConverterRegistry converterRegistry;
    private final List<RequestBodyValidator> validators;

    public RequestBodyArgumentResolver(HttpMessageConverterRegistry converterRegistry,
                                       List<RequestBodyValidator> validators) {
        this.converterRegistry = converterRegistry;
        this.validators = List.copyOf(validators);
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

        if (body != null) {
            for (RequestBodyValidator validator : validators) {
                validator.validate(body, parameter);
            }
        }

        return body;
    }
}
