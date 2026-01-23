package io.lumen.security;

import jakarta.servlet.http.HttpServletRequest;

public class AnyPathRequestMatcher implements RequestMatcher {
    @Override
    public boolean matches(HttpServletRequest request) {
        return true;
    }
}
