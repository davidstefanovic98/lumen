package io.lumen.web.argument;

import java.lang.reflect.Parameter;

/**
 * Validates a deserialized {@code @RequestBody} argument before it reaches the controller.
 *
 * <p>Every {@code RequestBodyValidator} light is applied, in turn, to every non-null request body.
 * Each implementation decides for itself whether a parameter is in scope (e.g. lumen-validation's
 * bridge only acts on parameters annotated with its own {@code @Valid}), so lumen-web never needs
 * to know which validation module - if any - is on the classpath.
 *
 * <p>Report a failed validation by throwing {@link io.lumen.web.exception.MethodArgumentNotValidException},
 * which lumen-web maps to a 422 {@link io.lumen.web.http.ErrorResponse} carrying the violations.
 * Register an implementation as an external light keyed by its concrete class.
 */
@FunctionalInterface
public interface RequestBodyValidator {

    void validate(Object body, Parameter parameter);
}
