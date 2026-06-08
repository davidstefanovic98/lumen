package io.lumen.security;

import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.authority.SimpleGrantedAuthority;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExceptionTranslationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    private static jakarta.servlet.http.HttpServletRequest apiRequest() {
        var req = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(req.getHeader("Accept")).thenReturn("application/json");
        when(req.getHeader("Content-Type")).thenReturn(null);
        when(req.getHeader("X-Requested-With")).thenReturn(null);
        return req;
    }

    private static jakarta.servlet.http.HttpServletRequest browserRequest() {
        var req = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(req.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml");
        when(req.getHeader("Content-Type")).thenReturn(null);
        when(req.getHeader("X-Requested-With")).thenReturn(null);
        return req;
    }

    private static jakarta.servlet.http.HttpServletResponse captureResponse() throws Exception {
        var resp = mock(jakarta.servlet.http.HttpServletResponse.class);
        var writer = mock(java.io.PrintWriter.class);
        when(resp.getWriter()).thenReturn(writer);
        return resp;
    }

    private static jakarta.servlet.FilterChain throwingChain(RuntimeException ex) {
        return (req, resp) -> { throw ex; };
    }

    // -------------------------------------------------------------------------
    // Form login disabled (REST API — the common case)
    // -------------------------------------------------------------------------

    @Test
    void formLoginDisabled_unauthenticated_apiRequest_returns401() throws Exception {
        var filter = new ExceptionTranslationFilter("/login", false);
        var response = captureResponse();

        filter.doFilterInternal(apiRequest(), response, throwingChain(new AccessDeniedException("denied")));

        verify(response).setStatus(401);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void formLoginDisabled_unauthenticated_browserRequest_returns401NotRedirect() throws Exception {
        var filter = new ExceptionTranslationFilter("/login", false);
        var response = captureResponse();

        filter.doFilterInternal(browserRequest(), response, throwingChain(new AccessDeniedException("denied")));

        verify(response).setStatus(401);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void formLoginDisabled_authenticated_forbidden_apiRequest_returns403() throws Exception {
        authenticate("USER");
        var filter = new ExceptionTranslationFilter("/login", false);
        var response = captureResponse();

        filter.doFilterInternal(apiRequest(), response, throwingChain(new AccessDeniedException("denied")));

        verify(response).setStatus(403);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void formLoginDisabled_authenticated_forbidden_browserRequest_renders403Page() throws Exception {
        authenticate("USER");
        var filter = new ExceptionTranslationFilter("/login", false);
        var response = captureResponse();

        filter.doFilterInternal(browserRequest(), response, throwingChain(new AccessDeniedException("denied")));

        verify(response).setStatus(403);
        verify(response, never()).sendRedirect(anyString());
    }

    // -------------------------------------------------------------------------
    // Form login enabled (browser / MVC app)
    // -------------------------------------------------------------------------

    @Test
    void formLoginEnabled_unauthenticated_browserRequest_redirectsToLogin() throws Exception {
        var filter = new ExceptionTranslationFilter("/login", true);
        var response = captureResponse();

        filter.doFilterInternal(browserRequest(), response, throwingChain(new AccessDeniedException("denied")));

        verify(response).sendRedirect("/login");
        verify(response, never()).setStatus(401);
    }

    @Test
    void formLoginEnabled_unauthenticated_apiRequest_returns401NotRedirect() throws Exception {
        var filter = new ExceptionTranslationFilter("/login", true);
        var response = captureResponse();

        filter.doFilterInternal(apiRequest(), response, throwingChain(new AccessDeniedException("denied")));

        verify(response).setStatus(401);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void formLoginEnabled_authenticated_forbidden_browserRequest_renders403Page() throws Exception {
        authenticate("USER");
        var filter = new ExceptionTranslationFilter("/login", true);
        var response = captureResponse();

        filter.doFilterInternal(browserRequest(), response, throwingChain(new AccessDeniedException("denied")));

        verify(response).setStatus(403);
        verify(response, never()).sendRedirect(anyString());
    }

    // -------------------------------------------------------------------------
    // Non-AccessDeniedException re-throws as ServletException
    // -------------------------------------------------------------------------

    @Test
    void nonAccessDeniedException_isRethrown() {
        var filter = new ExceptionTranslationFilter("/login", false);
        var ex = new IllegalStateException("unexpected");
        assertThrows(jakarta.servlet.ServletException.class,
                () -> filter.doFilterInternal(apiRequest(), captureResponse(), throwingChain(ex)));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private void authenticate(String... roles) {
        var authorities = List.of(roles).stream()
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, authorities));
    }
}