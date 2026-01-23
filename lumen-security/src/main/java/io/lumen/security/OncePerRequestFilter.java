package io.lumen.security;

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
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            throw new ServletException("OncePerRequestFilter only supports HTTP requests");
        }

        String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();

        if (request.getAttribute(alreadyFilteredAttributeName) != null) {
            chain.doFilter(request, response);
        } else {
            // Mark it as filtered
            request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
            try {
                doFilterInternal(httpRequest, httpResponse, chain);
            } finally {
                // We typically don't remove the attribute so it stays 'filtered'
                // throughout the entire request lifecycle (even during forwards)
            }
        }
    }

    private String getAlreadyFilteredAttributeName() {
        return this.getClass().getName() + ALREADY_FILTERED_SUFFIX;
    }

    protected abstract void doFilterInternal(HttpServletRequest request,
                                             HttpServletResponse response,
                                             FilterChain chain) throws ServletException, IOException;
}
