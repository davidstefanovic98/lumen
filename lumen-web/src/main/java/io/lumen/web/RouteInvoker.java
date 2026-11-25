package io.lumen.web;

import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.handler.ResponseBodyWriter;
import io.lumen.web.handler.RestResultHandler;
import io.lumen.web.handler.RouteResultHandler;
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
 *
 * <p>A {@link CompletableFuture} return value doesn't get its own response/exception pipeline.
 * When the future completes, its outcome is stored on the request and the request is re-dispatched
 * ({@link AsyncContext#dispatch()}); on that ASYNC dispatch {@link DispatcherServlet} calls
 * {@link #resumeAsync} inside its normal try/catch, so a value goes through the same result
 * handlers and an exception through the same exception path - including the filter chain, which
 * runs again for the ASYNC dispatch - as a synchronous return would.
 */
public class RouteInvoker {

    private static final String ASYNC_RESULT_ATTRIBUTE = RouteInvoker.class.getName() + ".ASYNC_RESULT";

    /** The outcome of a completed {@code CompletableFuture} handler, carried across the async re-dispatch. */
    private record AsyncResult(Route route, Object[] args, Object value, Throwable failure) {}

    private final CompositeMethodArgumentResolver argumentResolver;
    private final List<RouteResultHandler> resultHandlers = new ArrayList<>();

    public RouteInvoker(ResponseBodyWriter bodyWriter, CompositeMethodArgumentResolver argResolver) {
        this.argumentResolver = argResolver;
        resultHandlers.add(new RestResultHandler(bodyWriter));
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
            startAsync(future, route, args, request);
            return;
        }

        writeResult(returnValue, route, args, request, response);
    }

    private void startAsync(CompletableFuture<?> future, Route route, Object[] args, HttpServletRequest request) {
        AsyncContext asyncContext = request.startAsync();

        future.whenComplete((value, throwable) -> {
            asyncContext.getRequest().setAttribute(ASYNC_RESULT_ATTRIBUTE,
                    new AsyncResult(route, args, value, throwable == null ? null : unwrap(throwable)));
            asyncContext.dispatch();
        });
    }

    /**
     * Completes a request whose handler returned a {@code CompletableFuture}, on the ASYNC dispatch
     * that {@link #startAsync} triggered. Writes the value, or rethrows the failure exactly as the
     * handler would have thrown it synchronously.
     *
     * @return {@code false} if this request carries no async outcome (it isn't a resumption)
     */
    boolean resumeAsync(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!(request.getAttribute(ASYNC_RESULT_ATTRIBUTE) instanceof AsyncResult result)) {
            return false;
        }
        request.removeAttribute(ASYNC_RESULT_ATTRIBUTE);

        switch (result.failure()) {
            case null -> writeResult(result.value(), result.route(), result.args(), request, response);
            case Exception e -> throw e;
            // Same treatment invokeAndWrite() gives a non-Exception thrown synchronously
            default -> throw new RuntimeException(result.failure());
        }
        return true;
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable t = throwable;
        while (t instanceof CompletionException && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    private void writeResult(Object returnValue, Route route, Object[] args,
                              HttpServletRequest request, HttpServletResponse response) throws Exception {
        for (RouteResultHandler handler : resultHandlers) {
            if (handler.supports(returnValue, route)) {
                handler.handle(returnValue, args, route, request, response);
                return;
            }
        }

        if (returnValue == null) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }
}