package io.lumen.web;

import io.lumen.context.annotation.Component;
import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.handler.RestResultHandler;
import io.lumen.web.handler.RouteResultHandler;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Invokes a route method with resolved parameters.
 */
@Component
public class RouteInvoker {
    private final CompositeMethodArgumentResolver argumentResolver;
    private final List<RouteResultHandler> resultHandlers = new ArrayList<>();

    public RouteInvoker(HttpMessageConverterRegistry registry, CompositeMethodArgumentResolver argResolver) {
        this.argumentResolver = argResolver;
        resultHandlers.add(new RestResultHandler(registry));
    }

    public void addResultHandler(RouteResultHandler handler) {
        resultHandlers.add(handler);
    }

    void invokeAndWrite(RouteMatch match, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Route route = match.route();
        Object[] args = argumentResolver.resolveArguments(
                route.getMethod().getParameters(), request, response, match.pathVariables()
        );

        Object returnValue = route.getMethod().invoke(route.getController(), args);

        for (RouteResultHandler handler : resultHandlers) {
            if (handler.supports(returnValue, route)) {
                handler.handle(returnValue, args, request, response);
                return;
            }
        }

        if (returnValue == null) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }
}
