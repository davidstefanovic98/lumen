package io.lumen.web.exception;

import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.HttpStatus;

/**
 * Thrown when a path variable or request parameter value can't be converted to the target
 * method parameter type (e.g. a non-numeric string for an {@code int} parameter). This is a
 * client error, not a server error - {@code DefaultExceptionResolver} maps it to 400 via
 * {@link ResponseStatus} instead of falling through to the default 500.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TypeConversionException extends RuntimeException {
    public TypeConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}