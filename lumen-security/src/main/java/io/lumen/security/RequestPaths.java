package io.lumen.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the path a {@link RequestMatcher} should match against, consistently across
 * matcher implementations. Prefers {@code getServletPath()}, falling back to
 * {@code getPathInfo()} and finally the request URI with the context path stripped.
 */
final class RequestPaths {

    private RequestPaths() {}

    static String resolve(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getPathInfo();
        }

        if (path == null || path.isEmpty()) {
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
                path = uri.substring(contextPath.length());
            } else {
                path = uri;
            }
        }

        if (path == null || path.isEmpty()) {
            path = "/";
        } else if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path;
    }
}