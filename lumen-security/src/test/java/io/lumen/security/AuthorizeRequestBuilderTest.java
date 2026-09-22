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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizeRequestBuilderTest {

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

    private void authenticate(String... authorities) {
        var granted = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, granted));
    }

    @Test
    void hasRole_storesRoleWithRolePrefix() {
        List<AuthorizationRule> rules = new ArrayList<>();
        new AuthorizeRequestBuilder(rules).antMatchers("/admin/**").hasRole("ADMIN");

        assertEquals("ROLE_ADMIN", rules.get(0).requiredRole());
    }

    @Test
    void hasRole_doesNotDoublePrefix_whenAlreadyGivenWithRolePrefix() {
        List<AuthorizationRule> rules = new ArrayList<>();
        new AuthorizeRequestBuilder(rules).antMatchers("/admin/**").hasRole("ROLE_ADMIN");

        assertEquals("ROLE_ADMIN", rules.get(0).requiredRole());
    }

    @Test
    void regexMatcher_hasRole_alsoNormalizesRolePrefix() {
        List<AuthorizationRule> rules = new ArrayList<>();
        new AuthorizeRequestBuilder(rules).regexMatchers("/admin/.*").hasRole("ADMIN");

        assertEquals("ROLE_ADMIN", rules.get(0).requiredRole());
    }

    @Test
    void endToEnd_userWithConventionalRoleAuthority_isGrantedAccess() throws Exception {
        // Regression: an authority stored as "ROLE_ADMIN" (the conventional form
        // SimpleGrantedAuthority expects, and what method-security's hasRole() already matched
        // against) used to never satisfy .antMatchers(...).hasRole("ADMIN") at the HTTP layer,
        // because AuthorizeRequestBuilder stored the raw "ADMIN" string with no prefix.
        List<AuthorizationRule> rules = new ArrayList<>();
        new AuthorizeRequestBuilder(rules).antMatchers("/admin/**").hasRole("ADMIN");

        authenticate("ROLE_ADMIN");
        AuthorizationFilter filter = new AuthorizationFilter(rules);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestFor("/admin/dashboard"), mock(HttpServletResponse.class), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void endToEnd_userWithoutRequiredRole_isDenied() {
        List<AuthorizationRule> rules = new ArrayList<>();
        new AuthorizeRequestBuilder(rules).antMatchers("/admin/**").hasRole("ADMIN");

        authenticate("ROLE_USER");
        AuthorizationFilter filter = new AuthorizationFilter(rules);

        assertThrows(AccessDeniedException.class, () ->
                filter.doFilter(requestFor("/admin/dashboard"), mock(HttpServletResponse.class), mock(FilterChain.class)));
    }
}