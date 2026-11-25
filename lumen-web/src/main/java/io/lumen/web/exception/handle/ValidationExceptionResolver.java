package io.lumen.web.exception.handle;

import io.lumen.validation.ValidationException;
import io.lumen.web.http.ErrorResponse;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * Maps {@link ValidationException} from lumen-validation (an optional dependency) to 422.
 * Referencing its types directly is safe because {@link CompositeExceptionResolver} only
 * instantiates this class after confirming lumen-validation is on the classpath - the class
 * is never loaded otherwise.
 */
class ValidationExceptionResolver implements ExceptionResolver {

    private final ErrorResponseWriter writer;

    ValidationExceptionResolver(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        ValidationException ve = findInCauseChain(ex);
        if (ve == null) return false;

        List<ErrorResponse.FieldError> violations = ve.getViolations().stream()
                .map(v -> new ErrorResponse.FieldError(v.field(), v.message()))
                .toList();
        writer.write(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, ve.getMessage(), violations), req, resp);
        return true;
    }

    private ValidationException findInCauseChain(Throwable t) {
        while (t != null) {
            if (t instanceof ValidationException ve) return ve;
            t = t.getCause();
        }
        return null;
    }
}
