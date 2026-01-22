package io.lumen.security;

import jakarta.servlet.http.HttpServletRequest;

interface RequestMatcher {
    boolean matches(HttpServletRequest request);
}
