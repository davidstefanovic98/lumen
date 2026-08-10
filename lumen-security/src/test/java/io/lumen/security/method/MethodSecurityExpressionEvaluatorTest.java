package io.lumen.security.method;

import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.authority.SimpleGrantedAuthority;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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

    // --- hasAuthority ---

    @Test
    void hasAuthority_requiresExactMatch_noRolePrefix() {
        // authenticate() stores ROLE_ADMIN (helper adds prefix)
        authenticate("ADMIN");
        // hasRole auto-adds ROLE_ → 'ADMIN' matches 'ROLE_ADMIN'
        assertDoesNotThrow(() -> MethodSecurityExpressionEvaluator.check("hasRole('ADMIN')"));
        // hasAuthority is exact — 'ADMIN' alone does not match 'ROLE_ADMIN'
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("hasAuthority('ADMIN')"));
        // hasAuthority('ROLE_ADMIN') exact match → passes
        assertDoesNotThrow(() -> MethodSecurityExpressionEvaluator.check("hasAuthority('ROLE_ADMIN')"));
    }

    @Test
    void hasAuthority_throws_whenNotAuthenticated() {
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("hasAuthority('ROLE_ADMIN')"));
    }

    // --- hasAnyAuthority ---

    @Test
    void hasAnyAuthority_passes_whenUserHasOne() {
        authenticate("ADMIN");
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.check("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')"));
    }

    @Test
    void hasAnyAuthority_throws_whenUserHasNone() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("hasAnyAuthority('ROLE_READ_USERS', 'ROLE_WRITE_USERS')"));
    }

    // --- combined expressions ---

    @Test
    void and_passes_whenBothTrue() {
        authenticate("ADMIN");
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.check("hasRole('ADMIN') && isAuthenticated()"));
    }

    @Test
    void and_throws_whenOneFalse() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("hasRole('ADMIN') && isAuthenticated()"));
    }

    @Test
    void or_passes_whenSecondTrue() {
        authenticate("USER");
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.check("hasRole('ADMIN') || hasRole('USER')"));
    }

    @Test
    void negation_passes_whenInnerFalse() {
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.check("!isAuthenticated()"));
    }

    @Test
    void negation_throws_whenInnerTrue() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("!isAuthenticated()"));
    }

    // --- #authentication property navigation ---

    @Test
    void authenticationName_passesWhenMatches() {
        authenticate("USER");
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.check("#authentication.name == 'user'"));
    }

    @Test
    void authenticationName_throwsWhenDoesNotMatch() {
        authenticate("USER");
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("#authentication.name == 'other'"));
    }

    // --- checkPre with method parameter binding ---

    @Test
    void checkPre_bindsMethodParameters() throws NoSuchMethodException {
        authenticate("USER");
        Method method = Stubs.class.getMethod("transfer", String.class, Long.class);
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.checkPre("#toAccount == 'alice'", method, new Object[]{"alice", 100L}));
        assertThrows(AccessDeniedException.class, () ->
                MethodSecurityExpressionEvaluator.checkPre("#toAccount == 'alice'", method, new Object[]{"bob", 100L}));
    }

    @Test
    void checkPre_combinedParamAndRole() throws NoSuchMethodException {
        authenticate("ADMIN");
        Method method = Stubs.class.getMethod("transfer", String.class, Long.class);
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.checkPre(
                        "hasRole('ADMIN') || #toAccount == #authentication.name",
                        method, new Object[]{"alice", 50L}));
    }

    // --- checkPost with #returnObject ---

    @Test
    void checkPost_bindsReturnObject() throws NoSuchMethodException {
        authenticate("USER");
        Method method = Stubs.class.getMethod("transfer", String.class, Long.class);
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.checkPost("#returnObject == 'ok'", method, new Object[]{}, "ok"));
        assertThrows(AccessDeniedException.class, () ->
                MethodSecurityExpressionEvaluator.checkPost("#returnObject == 'ok'", method, new Object[]{}, "fail"));
    }

    @Test
    void checkPost_nullReturnObject_nullCheckPasses() throws NoSuchMethodException {
        authenticate("USER");
        Method method = Stubs.class.getMethod("transfer", String.class, Long.class);
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.checkPost("#returnObject == null", method, new Object[]{}, null));
    }

    @Test
    void checkPost_nullReturnObject_nullGuardPattern_passes() throws NoSuchMethodException {
        authenticate("USER");
        Method method = Stubs.class.getMethod("transfer", String.class, Long.class);
        // Common guard: allow if result is absent, otherwise check ownership
        assertDoesNotThrow(() ->
                MethodSecurityExpressionEvaluator.checkPost(
                        "#returnObject == null || #returnObject == 'alice'", method, new Object[]{}, null));
    }

    @Test
    void checkPost_nullReturnObject_nonNullCheck_throwsAccessDeniedException() throws NoSuchMethodException {
        authenticate("USER");
        Method method = Stubs.class.getMethod("transfer", String.class, Long.class);
        // Expression evaluates to false (null != 'expected') → AccessDeniedException, not an evaluation error
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                MethodSecurityExpressionEvaluator.checkPost("#returnObject == 'expected'", method, new Object[]{}, null));
        assertEquals("Access denied", ex.getMessage());
    }

    // --- unknown function → access denied ---

    @Test
    void unknownFunction_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class,
                () -> MethodSecurityExpressionEvaluator.check("unknownFunction()"));
    }

    // ── stubs ─────────────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    static class Stubs {
        public void transfer(String toAccount, Long amount) {}
    }
}