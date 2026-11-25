package io.lumen.web.http;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The body of every error response the framework writes itself - unmatched routes, media-type
 * mismatches, validation failures, {@link io.lumen.web.annotation.ResponseStatus @ResponseStatus}
 * exceptions and unhandled 500s all share this one shape, serialized through the regular
 * {@link HttpMessageConverterRegistry} rather than hand-built JSON strings.
 *
 * <pre>{@code
 * {"status":404,"error":"Not Found","message":"No route or static resource found for GET /x"}
 * {"status":422,"error":"Unprocessable Entity","message":"...","violations":[{"field":"name","message":"..."}]}
 * }</pre>
 *
 * {@code violations} is only present on validation failures.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String error, String message, List<FieldError> violations) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(HttpStatus status, String message) {
        return of(status, message, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, List<FieldError> violations) {
        return new ErrorResponse(status.value(), status.reason(),
                message != null ? message : status.reason(), violations);
    }
}
