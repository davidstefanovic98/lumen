package io.lumen.security.method;

import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.authority.SimpleGrantedAuthority;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MethodSecurityExpressionEvaluatorTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    private void authenticate(String... roles) {
        var authorities = List.of(roles).stream()
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, authorities));
    }

    // --- permitAll ---

    @Test
    void permitAll_neverThrows() {
        assertDoesNotThrow(() -> MethodSecurityExpressionEvaluator.check("permitAll()"));
    }

    // --- denyAll ---

    @Test
    void denyAll_alwaysThrows() {
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("denyAll()"));
    }

    // --- isAuthenticated ---

    @Test
    void isAuthenticated_passes_whenAuthenticated() {
        authenticate("USER");
        assertDoesNotThrow(() -> MethodSecurityExpressionEvaluator.check("isAuthenticated()"));
    }

    @Test
    void isAuthenticated_throws_whenNotAuthenticated() {
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("isAuthenticated()"));
    }

    // --- isAnonymous ---

    @Test
    void isAnonymous_passes_whenNoAuth() {
        assertDoesNotThrow(() -> MethodSecurityExpressionEvaluator.check("isAnonymous()"));
    }

    @Test
    void isAnonymous_throws_whenAuthenticated() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("isAnonymous()"));
    }

    // --- hasRole ---

    @Test
    void hasRole_passes_whenUserHasRole() {
        authenticate("ADMIN");
        assertDoesNotThrow(() -> MethodSecurityExpressionEvaluator.check("hasRole('ADMIN')"));
    }

    @Test
    void hasRole_prefixesROLE_automatically() {
        authenticate("ADMIN");
        assertDoesNotThrow(() -> MethodSecurityExpressionEvaluator.check("hasRole('ADMIN')"));
    }

    @Test
    void hasRole_throws_whenUserLacksRole() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("hasRole('ADMIN')"));
    }

    @Test
    void hasRole_throws_whenNotAuthenticated() {
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("hasRole('ADMIN')"));
    }

    // --- hasAnyRole ---

    @Test
    void hasAnyRole_passes_whenUserHasOneOfRoles() {
        authenticate("MANAGER");
        assertDoesNotThrow(
                () -> MethodSecurityExpressionEvaluator.check("hasAnyRole('ADMIN', 'MANAGER')"));
    }

    @Test
    void hasAnyRole_throws_whenUserHasNoneOfRoles() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("hasAnyRole('ADMIN', 'MANAGER')"));
    }

    // --- unsupported expression ---

    @Test
    void unsupportedExpression_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class,
                () -> MethodSecurityExpressionEvaluator.check("badExpression()"));
    }
}