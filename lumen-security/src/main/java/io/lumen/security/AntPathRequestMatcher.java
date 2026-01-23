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
        if (method != null && !method.matches(request.getMethod())) {
            return false;
        }

        String path = request.getPathInfo();

        if (path == null) {
            path = request.getServletPath();
        }

        if (pattern.equals("/**") || pattern.equals("/*")) {
            return true;
        }

        if (pattern.endsWith("/**")) {
            String base = pattern.replace("/**", "");
            return path.startsWith(base);
        }

        return path.equals(pattern);
    }
}
