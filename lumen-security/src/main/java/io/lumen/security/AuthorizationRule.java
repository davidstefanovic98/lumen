package io.lumen.security;

public record AuthorizationRule(RequestMatcher matcher, String requiredRole) {}
