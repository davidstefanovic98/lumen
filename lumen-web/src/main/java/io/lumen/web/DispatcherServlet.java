package io.lumen.web;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.exception.HttpMediaTypeNotAcceptableException;
import io.lumen.web.exception.HttpMediaTypeNotSupportedException;
import io.lumen.web.exception.NotFoundException;
import io.lumen.web.exception.handle.CompositeExceptionResolver;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.resource.ResourceProvider;
import io.lumen.web.resource.StaticResourceResultHandler;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

public class DispatcherServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);
    private final RouteRegistry registry;
    private final RouteInvoker invoker;
    private final CompositeExceptionResolver exceptionResolver;
    private final ResourceProvider resourceProvider;
    private final StaticResourceResultHandler resourceHandler;

    public DispatcherServlet(RouteRegistry registry,
                             ControllerAdviceRegistry adviceRegistry,
                             HttpMessageConverterRegistry converterRegistry,
                             RouteInvoker invoker,
                             ResourceProvider resourceProvider,
                             StaticResourceResultHandler resourceHandler) {
        this.registry = registry;
        this.invoker = invoker;
        this.resourceProvider = resourceProvider;
        this.resourceHandler = resourceHandler;
        this.exceptionResolver = new CompositeExceptionResolver(adviceRegistry, converterRegistry);
        logger.debug("DispatcherServlet initialized with {} routes", registry.getRouteCount());
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            path = "/index.html";
        } else if (path.endsWith("/")) {
            path = path + "index.html";
        }

        String method = req.getMethod();

        try {
            RouteMatch match = registry.findMatch(path, method);
            if (match == null) {
                ResourceProvider.StaticResource resource = resourceProvider.getResource(path);
                if (resource != null) {
                    resourceHandler.handle(resource, resp);
                    return;
                }
                throw new NotFoundException("No route or static resource found for " + method + " " + unquote(path));
            }

            Route route = match.route();

            if (requiresBody(method) && route.getConsumes() != null && route.getConsumes().length > 0) {
                String contentType = req.getContentType();
                if (contentType == null || !matchesMediaType(contentType, route.getConsumes())) {
                    throw new HttpMediaTypeNotSupportedException("Expected: " + String.join(", ", route.getConsumes()));
                }
            }

            if (route.getProduces() != null && route.getProduces().length > 0) {
                String accept = req.getHeader("Accept");
                if (accept != null && !accept.equals("*/*") && !matchesMediaType(accept, route.getProduces())) {
                    throw new HttpMediaTypeNotAcceptableException("Supported: " + String.join(", ", route.getProduces()));
                }
            }

            invoker.invokeAndWrite(match, req, resp);

        } catch (Exception e) {
            if (!exceptionResolver.resolve(req, resp, e)) {
                resp.sendError(500, "Unresolved error: " + e.getMessage());
            }
        }
    }

    private boolean requiresBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private boolean matchesMediaType(String headerValue, String[] supportedTypes) {
        return Arrays.stream(supportedTypes).anyMatch(headerValue::contains);
    }

    private String unquote(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("^\"|\"$", "");
    }
}