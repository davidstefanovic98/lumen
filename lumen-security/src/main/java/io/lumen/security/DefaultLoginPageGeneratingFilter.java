package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.http.HttpMethod;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Order(2)
public class DefaultLoginPageGeneratingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (HttpMethod.GET.matches(request.getMethod()) && "/login".equals(path)) {
            boolean hasError = request.getParameter("error") != null;
            boolean isLogout = request.getParameter("logout") != null;
            renderDefaultLoginPage(response, hasError, isLogout);
            return;
        }
        chain.doFilter(request, response);
    }

    private void renderDefaultLoginPage(HttpServletResponse response, boolean error, boolean logout) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String messageHtml = "";
        if (error) {
            messageHtml = "<div style='color: #ef4444; background: #fee2e2; padding: 0.75rem; border-radius: 6px; margin-bottom: 1rem; border: 1px solid #fecaca;'>Invalid credentials.</div>";
        } else if (logout) {
            messageHtml = "<div style='color: #065f46; background: #d1fae5; padding: 0.75rem; border-radius: 6px; margin-bottom: 1rem; border: 1px solid #a7f3d0;'>Successfully logged out.</div>";
        }

        String html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Lumen | Sign In</title>
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
                .login-card {
                    background: white;
                    padding: 2.5rem;
                    border-radius: 12px;
                    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
                    width: 100%;
                    max-width: 360px;
                }
                h2 {
                    margin: 0 0 0.5rem 0;
                    color: #111827;
                    font-size: 1.5rem;
                    font-weight: 700;
                }
                p {
                    color: #6b7280;
                    font-size: 0.875rem;
                    margin-bottom: 2rem;
                }
                .form-group {
                    margin-bottom: 1.25rem;
                }
                label {
                    display: block;
                    font-size: 0.875rem;
                    font-weight: 500;
                    color: #374151;
                    margin-bottom: 0.5rem;
                }
                input {
                    width: 100%;
                    padding: 0.75rem;
                    border: 1px solid #d1d5db;
                    border-radius: 6px;
                    box-sizing: border-box;
                    font-size: 1rem;
                    transition: border-color 0.2s, ring 0.2s;
                }
                input:focus {
                    outline: none;
                    border-color: #3b82f6;
                    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
                }
                button {
                    width: 100%;
                    background-color: #2563eb;
                    color: white;
                    padding: 0.75rem;
                    border: none;
                    border-radius: 6px;
                    font-size: 1rem;
                    font-weight: 600;
                    cursor: pointer;
                    transition: background-color 0.2s;
                    margin-top: 0.5rem;
                }
                button:hover {
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
            <div class="login-card">
                <h2>Login</h2>
                <p>Please sign in to your account</p>
                """ + messageHtml + """
                <form action="/login" method="POST">
                    <div class="form-group">
                        <label for="username">Username</label>
                        <input type="text" id="username" name="username" required autofocus>
                    </div>
                    <div class="form-group">
                        <label for="password">Password</label>
                        <input type="password" id="password" name="password" required>
                    </div>
                    <button type="submit">Sign In</button>
                </form>
                
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
