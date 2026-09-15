package io.lumen.security;

import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import io.lumen.security.manager.AuthenticationManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpSecurityTest {

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

    private AuthorizationFilter authorizationFilterOf(SecurityFilterChain chain) {
        return chain.getFilters().stream()
                .filter(AuthorizationFilter.class::isInstance)
                .map(AuthorizationFilter.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private SecurityRuleContributor permitAllContributor(String path) {
        return () -> List.of(new AuthorizationRule(new AntPathRequestMatcher(path), "PERMIT_ALL"));
    }

    @Test
    void build_appendsContributorRulesAfterUserRules() throws Exception {
        HttpSecurity http = new HttpSecurity(mock(AuthenticationManager.class), List.of(permitAllContributor("/actuator/health")));
        SecurityFilterChain chain = http
                .authorizeRequests(auth -> auth.antMatchers("/api/**").authenticated())
                .build();

        FilterChain servletChain = mock(FilterChain.class);
        authorizationFilterOf(chain).doFilter(requestFor("/actuator/health"), mock(HttpServletResponse.class), servletChain);

        verify(servletChain).doFilter(any(), any());
    }

    @Test
    void build_withoutContributors_onlyAppliesUserRules() {
        HttpSecurity http = new HttpSecurity(mock(AuthenticationManager.class), List.of());
        SecurityFilterChain chain = http
                .authorizeRequests(auth -> auth.antMatchers("/api/**").authenticated())
                .build();

        assertThrows(AccessDeniedException.class, () ->
                authorizationFilterOf(chain).doFilter(requestFor("/api/something"), mock(HttpServletResponse.class), mock(FilterChain.class)));
    }

    @Test
    void build_contributorsAreScopedToTheirOwnInstance_notSharedAcrossHttpSecurityInstances() throws Exception {
        // Regression test: HttpSecurity.CONTRIBUTORS used to be a JVM-global static list, so a
        // rule contributed for one HttpSecurity/context would leak into every other build().
        // Each HttpSecurity instance now only ever sees the contributors it was constructed with.
        HttpSecurity first = new HttpSecurity(mock(AuthenticationManager.class), List.of(permitAllContributor("/only-in-first")));
        HttpSecurity second = new HttpSecurity(mock(AuthenticationManager.class), List.of());

        SecurityFilterChain firstChain = first.authorizeRequests(auth -> auth.antMatchers("/api/**").authenticated()).build();
        SecurityFilterChain secondChain = second.authorizeRequests(auth -> auth.antMatchers("/api/**").authenticated()).build();

        FilterChain servletChain = mock(FilterChain.class);
        authorizationFilterOf(firstChain).doFilter(requestFor("/only-in-first"), mock(HttpServletResponse.class), servletChain);
        verify(servletChain).doFilter(any(), any());

        assertThrows(AccessDeniedException.class, () ->
                authorizationFilterOf(secondChain).doFilter(requestFor("/only-in-first"), mock(HttpServletResponse.class), mock(FilterChain.class)));
    }
}