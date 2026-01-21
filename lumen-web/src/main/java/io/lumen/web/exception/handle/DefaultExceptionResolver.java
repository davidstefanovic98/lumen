package io.lumen.web.exception.handle;

import io.lumen.web.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class DefaultExceptionResolver implements ExceptionResolver {

    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        try {
            resp.setContentType("application/json");
            resp.getWriter().write("{\"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"" + ex.getMessage() + "\"}");
        } catch (Exception ignored) {}
        return true;
    }
}
