package io.lumen.web;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.exception.HttpMediaTypeNotAcceptableException;
import io.lumen.web.exception.HttpMediaTypeNotSupportedException;
import io.lumen.web.exception.NotFoundException;
import io.lumen.web.exception.handle.CompositeExceptionResolver;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.flash.FlashMapManager;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.http.HttpMethod;
import io.lumen.web.resource.ResourceProvider;
import io.lumen.web.resource.StaticResourceResultHandler;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

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
        String contextPath = req.getContextPath();
        String path = req.getRequestURI().substring(contextPath.length());
        if (path.isEmpty())
            path = "/";

        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String method = req.getMethod();

        try {
            RouteMatch match = registry.findMatch(path, method);

            if (match != null) {
                handleControllerRequest(match, req, resp);
                return;
            }

            ResourceProvider.StaticResource resource = resourceProvider.getResource(path);

            if (resource == null && (path.equals("/") || path.isEmpty())) {
                resource = resourceProvider.getWelcomePage();
            }

            if (resource != null) {
                resourceHandler.handle(resource, req, resp);
                return;
            }

            throw new NotFoundException("No route or static resource found for " + method + " " + unquote(path));

        } catch (Exception e) {
            // Let security exceptions propagate so ExceptionTranslationFilter can handle them
            Throwable cause = e;
            while (cause != null) {
                if (cause.getClass().getName().equals("io.lumen.security.exception.AccessDeniedException")) {
                    throw new RuntimeException(e);
                }
                cause = cause.getCause();
            }
            if (!exceptionResolver.resolve(req, resp, e)) {
                logger.error("Unresolved error in DispatcherServlet", e);
                resp.sendError(500, "Internal Server Error");
            }
        }
    }

    private void handleControllerRequest(RouteMatch match, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Route route = match.route();
        String method = req.getMethod();

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

        Map<String, Object> flashAttributes = FlashMapManager.consume(req);
        if (flashAttributes != null) {
            req.setAttribute("LUMEN_FLASH_ATTRIBUTES", flashAttributes);
        }

        invoker.invokeAndWrite(match, req, resp);
    }

    private boolean requiresBody(String method) {
        return HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.PATCH.matches(method);
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