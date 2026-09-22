package io.lumen.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the path a {@link RequestMatcher} (or a security filter doing its own path check)
 * should match against, consistently everywhere in this module. Prefers {@code getPathInfo()},
 * falling back to {@code getServletPath()} and finally the request URI with the context path
 * stripped.
 * <p>
 * pathInfo-first, not servletPath-first, is the order that's correct across every servlet
 * mapping style: with a single path-mapped servlet ({@code /*}, how Lumen's own
 * {@code DispatcherServlet} is always registered) {@code getPathInfo()} is {@code null} and this
 * falls through to {@code getServletPath()}, which already holds the full path. With a
 * prefix-mapped servlet ({@code /api/*}) {@code getServletPath()} holds only the mapping prefix
 * ({@code "/api"}) and {@code getPathInfo()} holds the actual sub-path — preferring servletPath
 * there silently matches against the wrong string. servletPath-first only happens to look correct
 * for Lumen's own path-mapped DispatcherServlet, where pathInfo is always null anyway.
 */
final class RequestPaths {

    private RequestPaths() {}

    static String resolve(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isEmpty()) {
            path = request.getServletPath();
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