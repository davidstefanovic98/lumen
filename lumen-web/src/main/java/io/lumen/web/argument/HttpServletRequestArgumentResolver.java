package io.lumen.web.argument;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.Map;

class HttpServletRequestArgumentResolver implements MethodArgumentResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.getType() == HttpServletRequest.class;
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        return request;
    }
}
