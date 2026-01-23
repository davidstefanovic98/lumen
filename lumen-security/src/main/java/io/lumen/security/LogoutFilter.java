package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.security.context.SecurityContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@Order(4)
public class LogoutFilter extends OncePerRequestFilter {
    private final String logoutUrl;
    private final String logoutSuccessUrl;

    public LogoutFilter(String logoutUrl, String logoutSuccessUrl) {
        this.logoutUrl = logoutUrl;
        this.logoutSuccessUrl = logoutSuccessUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (requiresLogout(request)) {
            performLogout(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean requiresLogout(HttpServletRequest request) {
        return logoutUrl.equals(request.getRequestURI());
    }

    private void performLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clear();
        response.sendRedirect(logoutSuccessUrl);
    }
}
