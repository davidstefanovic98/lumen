package io.lumen.web.exception.handle;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class DefaultExceptionResolver implements ExceptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionResolver.class);

    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        logger.error("Unhandled exception for {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        try {
            resp.setContentType("application/json");
            resp.getWriter().write("{\"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"" + ex.getMessage() + "\"}");
        } catch (Exception ignored) {}
        return true;
    }
}
