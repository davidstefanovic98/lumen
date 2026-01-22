package io.lumen.web.argument;

import io.lumen.context.annotation.Component;
import io.lumen.web.exception.MethodArgumentResolveException;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class CompositeMethodArgumentResolver {

    private final List<MethodArgumentResolver> resolvers = new CopyOnWriteArrayList<>();

    public CompositeMethodArgumentResolver() {}

    public void addResolver(MethodArgumentResolver resolver) {
        resolvers.add(resolver);
    }

    public Object[] resolveArguments(Parameter[] parameters,
                                     HttpServletRequest request,
                                     HttpServletResponse response,
                                     Map<String, String> pathVariables) {
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            arguments[i] = resolveArgument(parameters[i], request, response, pathVariables);
        }
        return arguments;
    }

    private Object resolveArgument(
            Parameter parameter,
            HttpServletRequest request,
            HttpServletResponse response,
            Map<String, String> pathVariables
    ) {
        for (MethodArgumentResolver resolver : resolvers) {
            if (resolver.supports(parameter)) {
                return resolver.resolve(parameter, request, response, pathVariables);
            }
        }
        throw new MethodArgumentResolveException(
                "No argument resolver found for parameter: " +
                        parameter.getType().getName() + " " + parameter.getName() +
                        ". Ensure the parameter is annotated properly."
        );
    }
}
