package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.security.context.SecurityContext;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.repository.SecurityContextRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Order(1)
public class SecurityContextPersistenceFilter extends OncePerRequestFilter {

    private final SecurityContextRepository repository;

    public SecurityContextPersistenceFilter(SecurityContextRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        SecurityContext contextBeforeChain = repository.loadContext(request);

        try {
            SecurityContextHolder.setContext(contextBeforeChain);
            chain.doFilter(request, response);
        } finally {
            SecurityContext contextAfterChain = SecurityContextHolder.getContext();
            SecurityContextHolder.clear();
            repository.saveContext(contextAfterChain, request, response);
        }
    }
}
