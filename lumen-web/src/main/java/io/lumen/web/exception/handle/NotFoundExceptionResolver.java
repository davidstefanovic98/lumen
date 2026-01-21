package io.lumen.web.exception.handle;

import io.lumen.web.exception.NotFoundException;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

class NotFoundExceptionResolver implements ExceptionResolver {

    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        if (ex instanceof NotFoundException) {
            sendError(resp, HttpStatus.NOT_FOUND.value(), ex.getMessage());
            return true;
        }
        return false;
    }

    private void sendError(HttpServletResponse resp, int status, String message) {
        try {
            resp.setStatus(status);
            resp.setContentType("application/json");
            resp.getWriter().write(String.format("{\"status\": %d, \"error\": \"%s\"}", status, message));
        } catch (IOException ignored) {}
    }
}
