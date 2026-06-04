package io.lumen.security.annotation;

import java.lang.annotation.*;

/**
 * Checks a Gleam security expression before the annotated method is invoked.
 *
 * <p>Expressions are evaluated by the Gleam engine and may use:
 * <ul>
 *   <li>{@code hasRole('ADMIN')} — requires {@code ROLE_ADMIN} authority (prefix added automatically)</li>
 *   <li>{@code hasAnyRole('ADMIN', 'USER')} — requires any of the listed roles</li>
 *   <li>{@code hasAuthority('READ_USERS')} — requires exact authority string</li>
 *   <li>{@code hasAnyAuthority('READ_USERS', 'WRITE_USERS')} — requires any of the listed authorities</li>
 *   <li>{@code isAuthenticated()} — requires any authenticated principal</li>
 *   <li>{@code isAnonymous()} — requires no authentication</li>
 *   <li>{@code permitAll()} — always passes</li>
 *   <li>{@code denyAll()} — always fails</li>
 * </ul>
 *
 * <p>The following variables are available in expressions:
 * <ul>
 *   <li>{@code #authentication} — the current {@link io.lumen.security.authentication.Authentication}
 *       object; supports property navigation, e.g. {@code #authentication.name}</li>
 *   <li>{@code #paramName} — each method parameter by its declared name
 *       (requires {@code -parameters} compiler flag)</li>
 * </ul>
 *
 * <p>Gleam supports boolean operators ({@code &&}, {@code ||}, {@code !}),
 * comparisons ({@code ==}, {@code !=}, {@code <}, {@code >}, {@code <=}, {@code >=}),
 * and property/method chaining. Example:
 * <pre>{@code
 * @PreAuthorize("hasRole('ADMIN') || #userId == #authentication.name")
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreAuthorize {
    String value();
}