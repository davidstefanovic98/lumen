package io.lumen.web.argument;

import io.lumen.web.annotation.RequestParam;
import io.lumen.web.util.ObjectBinder;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

import static io.lumen.web.util.TypeConverter.convert;
import static io.lumen.web.util.TypeInspection.isSimpleType;

class RequestParamArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestParam.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, Map<String, String> pathVariables) {
        RequestParam rp = parameter.getAnnotation(RequestParam.class);
        Class<?> type = parameter.getType();

        if (isSimpleType(type)) {
            String value = request.getParameter(rp.value());
            if (value == null && !rp.defaultValue().isEmpty()) {
                value = rp.defaultValue();
            }
            return convert(value, type);
        }

        return ObjectBinder.bind(type, request);
    }
}
