package io.lumen.web.exception.handle;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.ErrorResponse;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Terminal resolver: maps an exception type annotated with {@link ResponseStatus} (including the
 * framework's own - {@code NotFoundException}, the media-type exceptions, {@code TypeConversionException})
 * to its declared status, and anything else to a logged 500.
 */
class DefaultExceptionResolver implements ExceptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionResolver.class);

    private final ErrorResponseWriter writer;

    DefaultExceptionResolver(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        ResponseStatus ann = ex.getClass().getAnnotation(ResponseStatus.class);
        if (ann != null) {
            String message = ann.reason().isEmpty() ? ex.getMessage() : ann.reason();
            writer.write(ErrorResponse.of(ann.value(), message), req, resp);
            return true;
        }
        logger.error("Unhandled exception for {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        writer.write(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()), req, resp);
        return true;
    }
}
