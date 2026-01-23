package io.lumen.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public class SecurityFilterChain {
    private final String pattern;
    private final List<SecuritySubFilter> filters;

    public SecurityFilterChain(String pattern, List<SecuritySubFilter> filters) {
        this.pattern = pattern;
        this.filters = filters;
    }

    public boolean matches(HttpServletRequest request) {
        // For now, a simple startWith check. Later add ant-style pattern matching.
        return request.getRequestURI().startsWith(pattern.replace("/**", ""));
    }

    public List<SecuritySubFilter> getFilters() {
        return filters;
    }
}
