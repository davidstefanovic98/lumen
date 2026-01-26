package io.lumen.security;

import io.lumen.web.http.HttpMethod;
import jakarta.servlet.http.HttpServletRequest;

public class AntPathRequestMatcher implements RequestMatcher {
    private final String pattern;
    private final HttpMethod method;

    public AntPathRequestMatcher(String pattern) {
        this(pattern, null);
    }

    public AntPathRequestMatcher(String pattern, HttpMethod method) {
        this.pattern = pattern;
        this.method = method;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (method != null && !method.name().equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = getRequestPath(request);
        String normalizedPattern = pattern.startsWith("/") ? pattern : "/" + pattern;

        if (normalizedPattern.equals("/**") || normalizedPattern.equals("/*")) {
            return true;
        }

        if (normalizedPattern.endsWith("/**")) {
            String base = normalizedPattern.substring(0, normalizedPattern.length() - 3);
            return path.equals(base) || path.startsWith(base + "/");
        }

        if (normalizedPattern.endsWith("/*")) {
            String base = normalizedPattern.substring(0, normalizedPattern.length() - 2);

            if (!path.startsWith(base + "/")) {
                return false;
            }

            String remainder = path.substring(base.length() + 1);
            return !remainder.contains("/");
        }

        return path.equals(normalizedPattern);
    }

    private String getRequestPath(HttpServletRequest request) {
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
