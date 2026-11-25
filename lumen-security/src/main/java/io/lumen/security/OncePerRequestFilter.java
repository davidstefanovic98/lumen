package io.lumen.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class OncePerRequestFilter implements SecuritySubFilter {

    private static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";

    @Override
    public int getOrder() {
        io.lumen.core.annotation.Order order =
                getClass().getAnnotation(io.lumen.core.annotation.Order.class);
        return order != null ? order.value() : Integer.MAX_VALUE;
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            throw new ServletException("OncePerRequestFilter only supports HTTP requests");
        }

        String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();

        if (request.getAttribute(alreadyFilteredAttributeName) != null
                && request.getDispatcherType() != DispatcherType.ASYNC) {
            chain.doFilter(request, response);
        } else {
            // The attribute is never removed, so the filter stays 'filtered' for the rest of the
            // request - nested FORWARD/INCLUDE dispatches and ERROR dispatches skip it. ASYNC is the
            // exception: a CompletableFuture handler's outcome is processed on a fresh ASYNC
            // dispatch, on a different thread, after the original filter invocations have already
            // returned. The chain has to run again there so SecurityContextPersistenceFilter
            // re-establishes the SecurityContext and ExceptionTranslationFilter can translate an
            // AccessDeniedException the future failed with - otherwise it would surface as a 500.
            request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
            doFilterInternal(httpRequest, httpResponse, chain);
        }
    }

    private String getAlreadyFilteredAttributeName() {
        return this.getClass().getName() + ALREADY_FILTERED_SUFFIX;
    }

    protected abstract void doFilterInternal(HttpServletRequest request,
                                             HttpServletResponse response,
                                             FilterChain chain) throws ServletException, IOException;
}
