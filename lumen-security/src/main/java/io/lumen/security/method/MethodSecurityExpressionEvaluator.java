package io.lumen.security.method;

import io.lumen.security.authentication.Authentication;
import io.lumen.security.authority.GrantedAuthority;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MethodSecurityExpressionEvaluator {

    private static final Pattern HAS_ROLE      = Pattern.compile("hasRole\\(['\"]([^'\"]+)['\"]\\)");
    private static final Pattern HAS_ANY_ROLE  = Pattern.compile("hasAnyRole\\(([^)]+)\\)");

    private MethodSecurityExpressionEvaluator() {}

    public static void check(String expression) {
        String expr = expression.trim();

        if (expr.equals("permitAll()"))      return;
        if (expr.equals("denyAll()"))        throw new AccessDeniedException("denyAll() expression");
        if (expr.equals("isAuthenticated()")) { requireAuthenticated(); return; }
        if (expr.equals("isAnonymous()"))    { requireAnonymous(); return; }

        Matcher hasRole = HAS_ROLE.matcher(expr);
        if (hasRole.matches()) {
            requireAuthenticated();
            requireRole(hasRole.group(1));
            return;
        }

        Matcher hasAnyRole = HAS_ANY_ROLE.matcher(expr);
        if (hasAnyRole.matches()) {
            requireAuthenticated();
            String[] roles = hasAnyRole.group(1).split(",");
            requireAnyRole(Arrays.stream(roles)
                    .map(r -> r.trim().replaceAll("['\"]", ""))
                    .toList());
            return;
        }

        throw new UnsupportedOperationException("Unsupported security expression: " + expression);
    }

    private static void requireAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new AccessDeniedException("Not authenticated");
    }

    private static void requireAnonymous() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated())
            throw new AccessDeniedException("Access denied for authenticated users");
    }

    private static void requireRole(String role) {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasIt = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
        if (!hasIt)
            throw new AccessDeniedException("Access denied — requires authority: " + authority);
    }

    private static void requireAnyRole(List<String> roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        boolean hasAny = roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .anyMatch(authorities::contains);
        if (!hasAny)
            throw new AccessDeniedException("Access denied — requires one of: " + roles);
    }
}