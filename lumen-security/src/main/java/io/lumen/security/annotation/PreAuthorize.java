package io.lumen.security.annotation;

import java.lang.annotation.*;

/**
 * Checks a security expression before the annotated method is invoked.
 *
 * <p>Supported expressions (Phase 2):
 * <ul>
 *   <li>{@code hasRole('ADMIN')} — requires ROLE_ADMIN authority</li>
 *   <li>{@code hasAnyRole('ADMIN','USER')} — requires any of the listed roles</li>
 *   <li>{@code isAuthenticated()} — requires any authenticated principal</li>
 *   <li>{@code isAnonymous()} — requires no authentication</li>
 *   <li>{@code permitAll()} — always passes</li>
 *   <li>{@code denyAll()} — always fails</li>
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreAuthorize {
    String value();
}