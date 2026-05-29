package io.lumen.security;

import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.authority.SimpleGrantedAuthority;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    private HttpServletRequest requestFor(String path) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(path);
        when(req.getServletPath()).thenReturn(path);
        when(req.getContextPath()).thenReturn("");
        return req;
    }

    private void authenticate(String... roles) {
        var authorities = List.of(roles).stream()
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, authorities));
    }

    private AuthorizationRule permitAll(String path) {
        return new AuthorizationRule(new AntPathRequestMatcher(path), "PERMIT_ALL");
    }

    private AuthorizationRule requireAuthenticated(String path) {
        return new AuthorizationRule(new AntPathRequestMatcher(path), "IS_AUTHENTICATED");
    }

    private AuthorizationRule requireRole(String path, String role) {
        return new AuthorizationRule(new AntPathRequestMatcher(path), "ROLE_" + role);
    }

    @Test
    void permitAll_allowsAnonymousAccess() throws Exception {
        AuthorizationFilter filter = new AuthorizationFilter(List.of(permitAll("/public")));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestFor("/public"), mock(HttpServletResponse.class), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void authenticated_requiresLogin() {
        AuthorizationFilter filter = new AuthorizationFilter(List.of(requireAuthenticated("/private")));

        assertThrows(AccessDeniedException.class,
                () -> filter.doFilter(requestFor("/private"), mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void authenticated_allowsLoggedInUser() throws Exception {
        authenticate("USER");
        AuthorizationFilter filter = new AuthorizationFilter(List.of(requireAuthenticated("/private")));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestFor("/private"), mock(HttpServletResponse.class), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void role_requiresSpecificRole() {
        authenticate("USER");
        AuthorizationFilter filter = new AuthorizationFilter(List.of(requireRole("/admin", "ADMIN")));

        assertThrows(AccessDeniedException.class,
                () -> filter.doFilter(requestFor("/admin"), mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void role_allowsUserWithCorrectRole() throws Exception {
        authenticate("ADMIN");
        AuthorizationFilter filter = new AuthorizationFilter(List.of(requireRole("/admin", "ADMIN")));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestFor("/admin"), mock(HttpServletResponse.class), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void noMatchingRule_throwsAccessDeniedException() {
        AuthorizationFilter filter = new AuthorizationFilter(List.of(permitAll("/other")));

        assertThrows(AccessDeniedException.class,
                () -> filter.doFilter(requestFor("/unknown"), mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void firstMatchingRuleWins() throws Exception {
        // permit /api/** before requiring auth — /api/public should be allowed
        AuthorizationFilter filter = new AuthorizationFilter(List.of(
                permitAll("/api/public"),
                requireAuthenticated("/api/**")
        ));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestFor("/api/public"), mock(HttpServletResponse.class), chain);

        verify(chain).doFilter(any(), any());
    }
}