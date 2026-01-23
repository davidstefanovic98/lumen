package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@Order(100)
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
                if ("PERMIT_ALL".equals(rule.requiredRole())) {
                    chain.doFilter(request, response);
                    return;
                }

                if (authentication == null || !authentication.isAuthenticated()) {
                    throw new AccessDeniedException("Authentication required");
                }

                boolean hasRole = authentication.getAuthorities().contains(rule.requiredRole());
                if (!hasRole) {
                    throw new AccessDeniedException("Insufficient permissions");
                }
                chain.doFilter(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
