package io.lumen.web;

import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.http.HttpStatus;
import io.lumen.web.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;

/**
 * Invokes a route method with resolved parameters.
 */
class RouteInvoker {
    private final CompositeMethodArgumentResolver argumentResolver;
    private final HttpMessageConverterRegistry converterRegistry;

    RouteInvoker(HttpMessageConverterRegistry registry) {
        this.converterRegistry = registry;
        this.argumentResolver = new CompositeMethodArgumentResolver(registry);
    }

    void invokeAndWrite(RouteMatch match, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Route route = match.route();
        Method method = route.getMethod();
        Object controller = route.getController();

        Object[] args = argumentResolver.resolveArguments(
                method.getParameters(),
                request,
                match.pathVariables()
        );

        Object returnValue = method.invoke(controller, args);

        if (returnValue == null) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        Object bodyToConvert;
        if (returnValue instanceof ResponseEntity<?> responseEntity) {
            response.setStatus(responseEntity.getStatus());
            responseEntity.getHeaders().forEach(response::setHeader);
            bodyToConvert = responseEntity.getBody();

            if (bodyToConvert == null)
                return;
        } else {
            bodyToConvert = returnValue;
            response.setStatus(HttpStatus.OK.value());
        }
        if (route.isRest()) {
            if (response.getContentType() == null) {
                String contentType = (route.getProduces() != null && route.getProduces().length > 0)
                        ? route.getProduces()[0]
                        : "application/json";
                response.setContentType(contentType);
            }

            converterRegistry.write(bodyToConvert, bodyToConvert.getClass(), response);
        } else {
            // render view logic
        }
    }
}
