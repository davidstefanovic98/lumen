package io.lumen.web.exception.handle;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class DefaultExceptionResolver implements ExceptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionResolver.class);

    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        ResponseStatus ann = ex.getClass().getAnnotation(ResponseStatus.class);
        if (ann != null) {
            int code = ann.value().value();
            String message = ann.reason().isEmpty() ? ex.getMessage() : ann.reason();
            writeJson(resp, code, ann.value().name().replace('_', ' '), message);
            return true;
        }
        logger.error("Unhandled exception for {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        writeJson(resp, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex.getMessage());
        return true;
    }

    private void writeJson(HttpServletResponse resp, int status, String error, String message) {
        resp.setStatus(status);
        try {
            resp.setContentType("application/json");
            String msg = message != null ? message.replace("\"", "\\\"") : error;
            resp.getWriter().write(
                    "{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + msg + "\"}");
        } catch (Exception ignored) {}
    }
}
