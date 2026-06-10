package io.lumen.security;

import io.lumen.core.annotation.Order;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.manager.AuthenticationManager;
import io.lumen.web.http.HttpMethod;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Order(3)
public class UsernamePasswordAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final String loginUrl;
    private final String defaultSuccessUrl;
    private final String failureUrl;

    public UsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager,
                                                String loginUrl,
                                                String defaultSuccessUrl,
                                                String failureUrl) {
        this.authenticationManager = authenticationManager;
        this.loginUrl = loginUrl;
        this.defaultSuccessUrl = defaultSuccessUrl;
        this.failureUrl = failureUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!requiresAuthentication(request)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
            Authentication authResult = authenticationManager.authenticate(authRequest);

            SecurityContextHolder.getContext().setAuthentication(authResult);
            onSuccessfulAuthentication(request, response, authResult);
        } catch (Exception failed) {
            SecurityContextHolder.clear();
            onUnsuccessfulAuthentication(request, response, failed);
        }
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null)
            path = request.getServletPath();
        return HttpMethod.POST.matches(request.getMethod()) && loginUrl.equals(path);
    }

    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException {
        var session = request.getSession(true);
        session.setAttribute("LUMEN_SECURITY_CONTEXT_KEY", SecurityContextHolder.getContext());
        response.sendRedirect(defaultSuccessUrl);
    }

    protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Exception failed) throws IOException {
        response.sendRedirect(failureUrl);
    }
}
