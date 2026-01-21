package io.lumen.web.exception.handle;

import io.lumen.web.exception.HttpMediaTypeNotAcceptableException;
import io.lumen.web.exception.HttpMediaTypeNotSupportedException;
import io.lumen.web.exception.HttpMessageConvertException;
import io.lumen.web.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

class MediaTypeExceptionResolver implements ExceptionResolver {
    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            sendError(resp, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ex.getMessage());
            return true;
        }
        if (ex instanceof HttpMediaTypeNotAcceptableException || ex instanceof HttpMessageConvertException) {
            sendError(resp, HttpStatus.NOT_ACCEPTABLE.value(), "Not Acceptable: " + ex.getMessage());
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
