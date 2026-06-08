package io.lumen.web;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.exception.handle.CompositeExceptionResolver;
import io.lumen.web.handler.RestResultHandler;
import io.lumen.web.handler.RouteResultHandler;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Invokes a route method with resolved parameters.
 */
public class RouteInvoker {

    private static final Logger logger = LoggerFactory.getLogger(RouteInvoker.class);

    private final CompositeMethodArgumentResolver argumentResolver;
    private final CompositeExceptionResolver exceptionResolver;
    private final List<RouteResultHandler> resultHandlers = new ArrayList<>();

    public RouteInvoker(HttpMessageConverterRegistry registry,
                        CompositeMethodArgumentResolver argResolver,
                        CompositeExceptionResolver exceptionResolver) {
        this.argumentResolver = argResolver;
        this.exceptionResolver = exceptionResolver;
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

        Object returnValue;
        try {
            returnValue = route.getMethod().invoke(route.getController(), args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Exception ex) throw ex;
            throw new RuntimeException(cause);
        }

        if (returnValue instanceof CompletableFuture<?> future) {
            handleAsync(future, route, args, request, response);
            return;
        }

        writeResult(returnValue, route, args, request, response);
    }

    private void handleAsync(CompletableFuture<?> future, Route route, Object[] args,
                              HttpServletRequest request, HttpServletResponse response) {
        AsyncContext asyncContext = request.startAsync();

        future.whenComplete((result, throwable) -> {
            try {
                if (throwable != null) {
                    Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
                    Exception ex = cause instanceof Exception e ? e : new RuntimeException(cause);
                    if (!exceptionResolver.resolve(
                            (HttpServletRequest) asyncContext.getRequest(),
                            (HttpServletResponse) asyncContext.getResponse(), ex)) {
                        logger.error("Unresolved async error", ex);
                        asyncContext.getResponse().setContentType("application/json;charset=UTF-8");
                        ((HttpServletResponse) asyncContext.getResponse()).setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                } else {
                    writeResult(result, route, args,
                            (HttpServletRequest) asyncContext.getRequest(),
                            (HttpServletResponse) asyncContext.getResponse());
                }
            } catch (Exception e) {
                logger.error("Error writing async response", e);
            } finally {
                asyncContext.complete();
            }
        });
    }

    private void writeResult(Object returnValue, Route route, Object[] args,
                              HttpServletRequest request, HttpServletResponse response) throws Exception {
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
