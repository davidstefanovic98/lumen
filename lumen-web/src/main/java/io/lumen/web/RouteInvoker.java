package io.lumen.web;

import io.lumen.web.argument.CompositeMethodArgumentResolver;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;

/**
 * Invokes a route method with resolved parameters.
 */
class RouteInvoker {
    private final CompositeMethodArgumentResolver compositeMethodArgumentResolver;

    RouteInvoker() {
        this.compositeMethodArgumentResolver = new CompositeMethodArgumentResolver();
    }

    Object invoke(RouteMatch match, HttpServletRequest request) throws Exception {
        Route route = match.route();
        Method method = route.method();
        Object controller = route.controller();

        Object[] args = compositeMethodArgumentResolver.resolveArguments(
                method.getParameters(),
                request,
                match.pathVariables()
        );
        return method.invoke(controller, args);
    }
}