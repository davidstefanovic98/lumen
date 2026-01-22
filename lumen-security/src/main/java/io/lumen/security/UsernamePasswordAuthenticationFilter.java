package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.manager.AuthenticationManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Order(3)
public class UsernamePasswordAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    // this will be configurable in a real implementation
    private String loginUrl = "/login";

    public UsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!requiresAuthentication(request)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(username, password);
            Authentication authResult = authenticationManager.authenticate(authRequest);

            SecurityContextHolder.getContext().setAuthentication(authResult);

            onSuccessfulAuthentication(request, response, authResult);

        } catch (Exception failed) {
            SecurityContextHolder.clear();
            onUnsuccessfulAuthentication(request, response, failed);
        }
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && loginUrl.equals(request.getPathInfo());
    }

    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException {
        response.sendRedirect("/");
    }

    protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Exception failed) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed: " + failed.getMessage());
    }
}
