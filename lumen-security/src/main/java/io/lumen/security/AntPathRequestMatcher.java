package io.lumen.security;

import jakarta.servlet.http.HttpServletRequest;

public class AntPathRequestMatcher implements RequestMatcher {
    private final String pattern;

    public AntPathRequestMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        // Simple startWith check for patterns ending with /**
        return request.getServletPath().startsWith(pattern.replace("/**", ""));
    }
}
