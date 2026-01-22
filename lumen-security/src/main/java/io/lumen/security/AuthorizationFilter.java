package io.lumen.security;

import io.lumen.security.authentication.Authentication;
import io.lumen.security.context.SecurityContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class AuthorizationFilter extends OncePerRequestFilter {

    private final List<AuthorizationRule> rules;

    public AuthorizationFilter(List<AuthorizationRule> rules) {
        this.rules = rules;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        for (AuthorizationRule rule : rules) {
            if (rule.matcher().matches(request)) {
                if (authentication == null || !authentication.isAuthenticated()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Full authentication is required");
                    return;
                }

                boolean hasRole = authentication.getAuthorities().contains(rule.requiredRole());
                if (!hasRole) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
