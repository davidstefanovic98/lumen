package io.lumen.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * A sub-filter within a Lumen security filter chain.
 * Implement this to inject custom authentication logic (e.g. JWT extraction)
 * at a specific position in the chain using {@link #getOrder()}.
 */
public interface SecuritySubFilter {

    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException;

    default int getOrder() {
        return Integer.MAX_VALUE;
    }
}