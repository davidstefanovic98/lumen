package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Order(2)
public class DefaultLoginPageGeneratingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLoginPageGeneratingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        logger.info("DefaultLoginPageGeneratingFilter processing request: {} {}", request.getMethod(), path);
        if ("GET".equalsIgnoreCase(request.getMethod()) && "/login".equals(path)) {
            renderDefaultLoginPage(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private void renderDefaultLoginPage(HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String html = "<html><head><title>Lumen Login</title></head>" +
                "<body style='font-family: sans-serif; display: flex; justify-content: center; padding-top: 50px;'>" +
                "<div style='border: 1px solid #ccc; padding: 20px; border-radius: 8px;'>" +
                "<h2>Lumen Security Login</h2>" +
                "<form action='/login' method='POST'>" +
                "<div><label>User: </label><input type='text' name='username'/></div><br/>" +
                "<div><label>Password: </label><input type='password' name='password'/></div><br/>" +
                "<button type='submit'>Sign In</button>" +
                "</form></div></body></html>";

        response.getWriter().write(html);
    }
}
