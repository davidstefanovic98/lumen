package io.lumen.web;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;

/**
 * Invokes a route method with resolved parameters.
 */
class RouteInvoker {
    private final ParameterResolver parameterResolver;

    RouteInvoker() {
        this.parameterResolver = new ParameterResolver();
    }

    Object invoke(RouteMatch match, HttpServletRequest request) throws Exception {
        Route route = match.route();
        Method method = route.method();
        Object controller = route.controller();

        Object[] args = parameterResolver.resolveParameters(
                method.getParameters(),
                request,
                match.pathVariables()
        );

        return method.invoke(controller, args);
    }
}