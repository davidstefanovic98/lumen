package io.lumen.web.exception.handle;

import io.lumen.web.exception.MethodArgumentNotValidException;
import io.lumen.web.http.ErrorResponse;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Maps {@link MethodArgumentNotValidException} to a 422 {@link ErrorResponse} carrying its field
 * errors. A dedicated resolver rather than {@link DefaultExceptionResolver}'s generic
 * {@code @ResponseStatus} path only because the body needs the {@code violations} list.
 */
class MethodArgumentNotValidExceptionResolver implements ExceptionResolver {

    private final ErrorResponseWriter writer;

    MethodArgumentNotValidExceptionResolver(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        MethodArgumentNotValidException invalid = findInCauseChain(ex);
        if (invalid == null) return false;

        writer.write(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, invalid.getMessage(), invalid.getFieldErrors()),
                req, resp);
        return true;
    }

    private MethodArgumentNotValidException findInCauseChain(Throwable t) {
        while (t != null) {
            if (t instanceof MethodArgumentNotValidException invalid) return invalid;
            if (t.getCause() == t) break;
            t = t.getCause();
        }
        return null;
    }
}
