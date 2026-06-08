package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Order(99)
public class ExceptionTranslationFilter extends OncePerRequestFilter {

    private final String loginPage;
    private final boolean formLoginEnabled;

    public ExceptionTranslationFilter(String loginPage, boolean formLoginEnabled) {
        this.loginPage = loginPage;
        this.formLoginEnabled = formLoginEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null && !(cause instanceof AccessDeniedException)) {
                cause = cause.getCause();
            }
            if (cause instanceof AccessDeniedException denied) {
                handleAccessDenied(request, response, denied);
            } else {
                throw new ServletException(e);
            }
        }
    }

    private void handleAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                    AccessDeniedException ignored) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isApi = isApiRequest(request);

        if (auth == null || !auth.isAuthenticated()) {
            if (formLoginEnabled && !isApi) {
                response.sendRedirect(loginPage);
            } else {
                sendJson(response, HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Authentication required");
            }
        } else {
            if (isApi) {
                sendJson(response, HttpStatus.FORBIDDEN.value(), "Forbidden", "Access denied");
            } else {
                renderAccessDeniedPage(response);
            }
        }
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");
        String xhr = request.getHeader("X-Requested-With");
        return (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(xhr);
    }

    private void sendJson(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String msg = message != null ? message.replace("\"", "\\\"") : error;
        response.getWriter().write(
                "{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + msg + "\"}");
    }

    private void renderAccessDeniedPage(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write("""
                <!DOCTYPE html><html lang="en"><head><meta charset="UTF-8">
                <title>403 – Access Denied</title>
                <style>body{font-family:system-ui,sans-serif;display:flex;align-items:center;
                justify-content:center;height:100vh;margin:0;background:#f3f4f6}
                .card{background:#fff;padding:2.5rem;border-radius:12px;
                box-shadow:0 10px 25px rgba(0,0,0,.05);max-width:400px;text-align:center}
                h2{color:#111827}p{color:#6b7280}a{color:#2563eb}</style></head>
                <body><div class="card"><h2>403 – Access Denied</h2>
                <p>You don't have permission to access this resource.</p>
                <a href="/">Return Home</a></div></body></html>
                """);
    }
}