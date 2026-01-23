package io.lumen.security.repository;

import io.lumen.security.context.DefaultSecurityContext;
import io.lumen.security.context.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HttpSessionSecurityContextRepository implements SecurityContextRepository {
    private static final String LUMEN_SECURITY_CONTEXT_KEY = "LUMEN_SECURITY_CONTEXT_KEY";

    @Override
    public SecurityContext loadContext(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            SecurityContext context = (SecurityContext) session.getAttribute(LUMEN_SECURITY_CONTEXT_KEY);
            if (context != null) return context;
        }
        return new DefaultSecurityContext();
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        if (context.getAuthentication() != null) {
            var session = request.getSession(true);
            session.setAttribute(LUMEN_SECURITY_CONTEXT_KEY, context);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        var session = request.getSession(false);
        return session != null && session.getAttribute(LUMEN_SECURITY_CONTEXT_KEY) != null;
    }
}
