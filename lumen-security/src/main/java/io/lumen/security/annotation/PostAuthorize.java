package io.lumen.security.annotation;

import java.lang.annotation.*;

/**
 * Checks a Gleam security expression after the annotated method returns.
 *
 * <p>Supports the same expressions and variables as {@link PreAuthorize}, plus:
 * <ul>
 *   <li>{@code #returnObject} — the value returned by the method</li>
 * </ul>
 *
 * <p>Example — only allow access when the returned resource belongs to the caller:
 * <pre>{@code
 * @PostAuthorize("#returnObject.ownerId == #authentication.name")
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PostAuthorize {
    String value();
}