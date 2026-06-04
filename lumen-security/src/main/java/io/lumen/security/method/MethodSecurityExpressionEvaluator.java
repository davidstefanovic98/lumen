package io.lumen.security.method;

import io.lumen.gleam.CachingExpressionParser;
import io.lumen.gleam.Gleam;
import io.lumen.gleam.StandardEvaluationContext;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.authority.GrantedAuthority;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public final class MethodSecurityExpressionEvaluator {

    private static final CachingExpressionParser PARSER = Gleam.newCachingParser();

    private MethodSecurityExpressionEvaluator() {}

    /** Evaluates a {@code @PreAuthorize} expression, binding method parameters as variables. */
    public static void checkPre(String expression, Method method, Object[] args) {
        evaluate(expression, buildContext(method, args, null));
    }

    /** Evaluates a {@code @PostAuthorize} expression; return value is available as {@code #returnObject}. */
    public static void checkPost(String expression, Method method, Object[] args, Object returnValue) {
        StandardEvaluationContext ctx = buildContext(method, args, returnValue);
        evaluate(expression, ctx);
    }

    /**
     * Backward-compatible single-argument form (no method parameters in context).
     * Used when caller does not have access to the method reflection object.
     */
    public static void check(String expression) {
        evaluate(expression, buildContext(null, null, null));
    }

    // ── implementation ────────────────────────────────────────────────────────

    private static void evaluate(String expression, StandardEvaluationContext ctx) {
        Object result;
        try {
            result = PARSER.parse(expression).evaluate(ctx);
        } catch (io.lumen.gleam.EvaluationException e) {
            throw new AccessDeniedException("Security expression evaluation failed: " + e.getMessage());
        }
        if (!isTruthy(result)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private static StandardEvaluationContext buildContext(Method method, Object[] args, Object returnValue) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        StandardEvaluationContext ctx = new StandardEvaluationContext();

        // Bind method parameters as named variables (#paramName)
        if (method != null && args != null) {
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length && i < args.length; i++) {
                ctx.setVariable(params[i].getName(), args[i]);
            }
        }

        // Expose return value for @PostAuthorize
        if (returnValue != null) {
            ctx.setVariable("returnObject", returnValue);
        }

        // Expose authentication object
        if (auth != null) {
            ctx.setVariable("authentication", auth);
        }

        // Register security functions
        ctx.registerFunction("permitAll",      a -> true);
        ctx.registerFunction("denyAll",        a -> false);
        ctx.registerFunction("isAuthenticated",a -> auth != null && auth.isAuthenticated());
        ctx.registerFunction("isAnonymous",    a -> auth == null || !auth.isAuthenticated());

        ctx.registerFunction("hasRole", a -> {
            if (auth == null || !auth.isAuthenticated()) return false;
            String role = (String) a[0];
            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority::equals);
        });

        ctx.registerFunction("hasAnyRole", a -> {
            if (auth == null || !auth.isAuthenticated()) return false;
            List<String> authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList();
            return Arrays.stream(a)
                    .map(r -> { String s = (String) r; return s.startsWith("ROLE_") ? s : "ROLE_" + s; })
                    .anyMatch(authorities::contains);
        });

        ctx.registerFunction("hasAuthority", a -> {
            if (auth == null || !auth.isAuthenticated()) return false;
            String authority = (String) a[0];
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority::equals);
        });

        ctx.registerFunction("hasAnyAuthority", a -> {
            if (auth == null || !auth.isAuthenticated()) return false;
            List<String> authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList();
            return Arrays.stream(a)
                    .map(r -> (String) r)
                    .anyMatch(authorities::contains);
        });

        return ctx;
    }

    private static boolean isTruthy(Object v) {
        if (v == null)              return false;
        if (v instanceof Boolean b) return b;
        return true;
    }
}