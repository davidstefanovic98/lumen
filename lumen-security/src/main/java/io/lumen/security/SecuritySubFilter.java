package io.lumen.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * A sub-filter for security processing in the Lumen framework.
 * User's custom security filters should implement this interface.
 */
interface SecuritySubFilter {

    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException;

    default int getOrder() {
        return Integer.MAX_VALUE;
    }
}
