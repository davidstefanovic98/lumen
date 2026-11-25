package io.lumen.web.exception.handle;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.handler.ResponseBodyWriter;
import io.lumen.web.http.ErrorResponse;
import io.lumen.web.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the built-in resolvers' {@link ErrorResponse} bodies. A failure to write the body (e.g.
 * the client disconnected) is logged rather than propagated - the resolver has already decided
 * the outcome, and the status is set before the body write is attempted, so it survives either way.
 */
final class ErrorResponseWriter {

    private static final Logger logger = LoggerFactory.getLogger(ErrorResponseWriter.class);

    private final ResponseBodyWriter bodyWriter;

    ErrorResponseWriter(ResponseBodyWriter bodyWriter) {
        this.bodyWriter = bodyWriter;
    }

    void write(ErrorResponse body, HttpServletRequest req, HttpServletResponse resp) {
        try {
            bodyWriter.write(ResponseEntity.status(body.status()).body(body), req, resp);
        } catch (RuntimeException e) {
            logger.warn("Failed to write error response body (status {}): {}", body.status(), e.getMessage(), e);
        }
    }
}
