package io.lumen.web.argument;

import io.lumen.core.util.ParameterNameDiscoverer;
import io.lumen.web.annotation.RequestParam;
import io.lumen.web.exception.MissingRequestParameterException;
import io.lumen.web.exception.PrimitiveTypeRequestParameterException;
import io.lumen.web.util.ObjectBinder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jdk.dynalink.linker.support.TypeUtilities;

import java.lang.reflect.Parameter;
import java.util.Map;

import static io.lumen.web.util.TypeConverter.convert;
import static io.lumen.web.util.TypeInspection.isSimpleType;

public class RequestParamArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestParam.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        RequestParam rp = parameter.getAnnotation(RequestParam.class);
        Class<?> type = parameter.getType();

        String name = rp.value().isEmpty()
                ? ParameterNameDiscoverer.getParameterName(parameter)
                : rp.value();
        String value = request.getParameter(name);

        if (value == null && !rp.defaultValue().isEmpty()) {
            value = rp.defaultValue();
        }

        if (value == null && rp.required()) {
            throw new MissingRequestParameterException(String.format(
                    "Missing required request parameter '%s' for method parameter of type %s",
                    name,
                    type.getSimpleName()
            ));
        }

        if (value == null && type.isPrimitive()) {
            throw new PrimitiveTypeRequestParameterException(String.format(
                    "Optional parameter '%s' is missing but cannot be translated into a null value " +
                            "due to being declared as primitive type '%s'. " +
                            "Consider declaring it as wrapper type '%s' instead.",
                    name,
                    type.getSimpleName(),
                    TypeUtilities.getWrapperType(type).getSimpleName()
            ));
        }

        if (isSimpleType(type)) {
            return convert(value, type);
        }
        return ObjectBinder.bind(type, request);
    }
}
