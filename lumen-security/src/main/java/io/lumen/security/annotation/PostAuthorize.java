package io.lumen.security.annotation;

import java.lang.annotation.*;

/**
 * Checks a security expression after the annotated method returns.
 *
 * <p>Supports the same expressions as {@link PreAuthorize}.
 * Note: {@code returnObject} access is not yet supported (Phase 3).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PostAuthorize {
    String value();
}