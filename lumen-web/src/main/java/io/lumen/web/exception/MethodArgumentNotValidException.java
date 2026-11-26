package io.lumen.web.exception;

import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.ErrorResponse;
import io.lumen.web.http.HttpStatus;

import java.util.List;

/**
 * Thrown by a {@link io.lumen.web.argument.RequestBodyValidator} when a request body fails
 * validation. lumen-web's own type for "the client sent an invalid body" - validation modules
 * translate their native failure into it (keeping the original as the cause), so lumen-web can
 * render a 422 with the field errors without depending on any of them.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class MethodArgumentNotValidException extends RuntimeException {

    private final List<ErrorResponse.FieldError> fieldErrors;

    public MethodArgumentNotValidException(List<ErrorResponse.FieldError> fieldErrors, Throwable cause) {
        super("Validation failed with " + fieldErrors.size() + " violation(s)", cause);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<ErrorResponse.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
