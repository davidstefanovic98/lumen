package io.lumen.security.authority;

/**
 * Shared "ROLE_" prefixing rule for {@code hasRole()}/{@code hasAnyRole()}, used identically by
 * both the method-security layer ({@code MethodSecurityExpressionEvaluator}'s Gleam functions)
 * and the HTTP-filter layer ({@code AuthorizeRequestBuilder}/{@code AuthorizationFilter}).
 * <p>
 * Before this existed, the two layers implemented the same "does this role name need a ROLE_
 * prefix" concept independently and had drifted: method security prefixed, the HTTP layer did
 * not, so an authority stored as {@code "ROLE_ADMIN"} (the conventional form
 * {@link SimpleGrantedAuthority} expects, and what method security's {@code hasRole} matches
 * against) would never satisfy {@code .antMatchers(...).hasRole("ADMIN")} at the HTTP layer.
 */
public final class RoleAuthorities {

    private static final String PREFIX = "ROLE_";

    private RoleAuthorities() {}

    public static String normalize(String role) {
        return role.startsWith(PREFIX) ? role : PREFIX + role;
    }
}