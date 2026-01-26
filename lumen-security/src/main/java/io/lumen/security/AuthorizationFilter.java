package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;

@Order(100)
public class AuthorizationFilter implements SecuritySubFilter {
    private final List<AuthorizationRule> rules;

    public AuthorizationFilter(List<AuthorizationRule> rules) {
        this.rules = rules;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        for (AuthorizationRule rule : rules) {
            if (rule.matcher().matches((HttpServletRequest) request)) {
                String role = rule.requiredRole();

                if ("PERMIT_ALL".equals(role)) {
                    chain.doFilter(request, response);
                    return;
                }

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated()) {
                    throw new AccessDeniedException("Not logged in");
                }

                if (!"IS_AUTHENTICATED".equals(role)) {
                    boolean hasRole = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals(role));
                    if (!hasRole) {
                        throw new AccessDeniedException("Insufficient roles");
                    }
                }

                chain.doFilter(request, response);
                return;
            }
        }
        throw new AccessDeniedException("No matching rule found");
    }
}
