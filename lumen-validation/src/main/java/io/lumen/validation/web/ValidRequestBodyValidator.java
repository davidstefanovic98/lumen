package io.lumen.validation.web;

import io.lumen.core.util.ReflectionUtil;
import io.lumen.validation.ConstraintViolation;
import io.lumen.validation.ValidationException;
import io.lumen.validation.Validator;
import io.lumen.validation.annotation.Valid;
import io.lumen.web.argument.RequestBodyValidator;
import io.lumen.web.exception.MethodArgumentNotValidException;
import io.lumen.web.http.ErrorResponse;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.function.Supplier;

/**
 * Bridges lumen-validation into lumen-web: validates {@code @Valid @RequestBody} arguments with
 * the container's {@link Validator} light and reports violations as lumen-web's
 * {@link MethodArgumentNotValidException} (422), keeping the original {@link ValidationException}
 * as its cause so an {@code @ExceptionHandler(ValidationException.class)} still matches.
 *
 * <p>Registered by {@link io.lumen.validation.LumenValidationModule} only when lumen-web is on the
 * classpath - this class is never loaded otherwise.
 */
public class ValidRequestBodyValidator implements RequestBodyValidator {

    // Resolved lazily: whichever Validator light the container ends up resolving (e.g. a user's
    // @Primary one) is only known after initialization, not when the module registers this.
    private final Supplier<Validator> validator;

    public ValidRequestBodyValidator(Supplier<Validator> validator) {
        this.validator = validator;
    }

    @Override
    public void validate(Object body, Parameter parameter) {
        // By name, not isAnnotationPresent(): classloader-safe, like every other annotation probe
        // on the request path (see ReflectionUtil).
        if (!ReflectionUtil.hasParameterAnnotationByName(parameter, Valid.class.getName())) return;

        List<ConstraintViolation> violations = validator.get().validate(body);
        if (violations.isEmpty()) return;

        List<ErrorResponse.FieldError> fieldErrors = violations.stream()
                .map(v -> new ErrorResponse.FieldError(v.field(), v.message()))
                .toList();
        throw new MethodArgumentNotValidException(fieldErrors, new ValidationException(violations));
    }
}
