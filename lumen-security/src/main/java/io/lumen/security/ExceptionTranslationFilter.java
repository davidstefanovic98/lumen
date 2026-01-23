package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.web.http.HttpStatus;
import io.lumen.web.http.MediaType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

@Order(99)
public class ExceptionTranslationFilter extends OncePerRequestFilter {

    private final String loginPage;

    public ExceptionTranslationFilter(String loginPage) {
        this.loginPage = loginPage;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            Throwable rootCause = e;
            while (rootCause.getCause() != null && !(rootCause instanceof AccessDeniedException)) {
                rootCause = rootCause.getCause();
            }

            if (rootCause instanceof AccessDeniedException) {
                handleException(request, response, (AccessDeniedException) rootCause);
            } else {
                throw new ServletException(e);
            }
        }
    }

    private void handleException(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            response.sendRedirect(loginPage);
        } else {
            renderAccessDeniedPage(response);
        }
    }

    private void renderAccessDeniedPage(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.TEXT_PLAIN.toString() + "; charset=UTF-8");

        String html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Lumen | Access Denied</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    background-color: #f3f4f6;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    height: 100vh;
                    margin: 0;
                }
                .error-card {
                    background: white;
                    padding: 2.5rem;
                    border-radius: 12px;
                    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
                    width: 100%;
                    max-width: 400px;
                    text-align: center;
                }
                .icon {
                    background-color: #fee2e2;
                    color: #ef4444;
                    width: 64px;
                    height: 64px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 32px;
                    margin: 0 auto 1.5rem auto;
                }
                h2 {
                    margin: 0 0 0.5rem 0;
                    color: #111827;
                    font-size: 1.5rem;
                    font-weight: 700;
                }
                p {
                    color: #6b7280;
                    font-size: 1rem;
                    line-height: 1.5;
                    margin-bottom: 2rem;
                }
                .btn {
                    display: inline-block;
                    background-color: #2563eb;
                    color: white;
                    padding: 0.75rem 1.5rem;
                    text-decoration: none;
                    border-radius: 6px;
                    font-size: 1rem;
                    font-weight: 600;
                    transition: background-color 0.2s;
                }
                .btn:hover {
                    background-color: #1d4ed8;
                }
                .footer {
                    margin-top: 1.5rem;
                    text-align: center;
                    font-size: 0.75rem;
                    color: #9ca3af;
                }
            </style>
        </head>
        <body>
            <div class="error-card">
                <div class="icon">!</div>
                <h2>403 - Access Denied</h2>
                <p>You don't have permission to access this resource. Please contact your administrator if you think this is a mistake.</p>
                
                <a href="/" class="btn">Return Home</a>
                
                <div class="footer">
                    Powered by Lumen Framework
                </div>
            </div>
        </body>
        </html>
        """;
        response.getWriter().write(html);
    }
}
