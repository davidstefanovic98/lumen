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

        String path = RequestPaths.resolve(request);
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
}
