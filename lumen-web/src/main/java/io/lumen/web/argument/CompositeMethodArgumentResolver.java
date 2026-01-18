package io.lumen.web.argument;

import io.lumen.web.exception.MethodArgumentResolveException;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

public class CompositeMethodArgumentResolver {

    private final List<MethodArgumentResolver> resolvers;

    public CompositeMethodArgumentResolver() {
        this.resolvers = List.of(
                new PathVariableArgumentResolver(),
                new RequestParamArgumentResolver(),
                new RequestBodyArgumentResolver(new HttpMessageConverterRegistry()),
                new ModelAttributeArgumentResolver()
        );
    }

    public Object[] resolveArguments(Parameter[] parameters,
                                     HttpServletRequest request,
                                     Map<String, String> pathVariables) {
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            arguments[i] = resolveArgument(parameters[i], request, pathVariables);
        }
        return arguments;
    }

    private Object resolveArgument(
            Parameter parameter,
            HttpServletRequest request,
            Map<String, String> pathVariables
    ) {
        for (MethodArgumentResolver resolver : resolvers) {
            if (resolver.supports(parameter)) {
                return resolver.resolve(parameter, request, pathVariables);
            }
        }
        throw new MethodArgumentResolveException(
                "No argument resolver found for parameter: " +
                        parameter.getType().getName() + " " + parameter.getName() +
                        ". Ensure the parameter is annotated properly."
        );
    }
}
