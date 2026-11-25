package io.lumen.web.handler;

import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.HttpMessageConverter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.http.HttpStatus;
import io.lumen.web.http.MediaType;
import io.lumen.web.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;

/**
 * The single place that turns a handler's return value into status + headers + a
 * content-negotiated body. Controller routes ({@link RestResultHandler}), {@code @ExceptionHandler}
 * methods, and the framework's own built-in error responses all write through this, so the same
 * return value produces the same HTTP response no matter which path produced it.
 */
public class ResponseBodyWriter {

    private final HttpMessageConverterRegistry converterRegistry;

    public ResponseBodyWriter(HttpMessageConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    /**
     * Writes the return value of {@code handlerMethod}. A {@link ResponseEntity} carries its own
     * status, which always wins. Otherwise the status comes from {@code @ResponseStatus} on the
     * method, defaulting to 200 when there is a body and 204 when there isn't.
     */
    public void write(Object result, Method handlerMethod, HttpServletRequest req, HttpServletResponse resp) {
        if (result instanceof ResponseEntity<?> entity) {
            write(entity, req, resp);
            return;
        }

        ResponseStatus declared = handlerMethod.getAnnotation(ResponseStatus.class);
        if (result == null) {
            resp.setStatus(declared != null ? declared.value().value() : HttpStatus.NO_CONTENT.value());
            return;
        }

        resp.setStatus(declared != null ? declared.value().value() : HttpStatus.OK.value());
        writeBody(result, req, resp);
    }

    public void write(ResponseEntity<?> entity, HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(entity.getStatus());
        entity.getHeaders().forEach(resp::setHeader);
        if (entity.getBody() != null) {
            writeBody(entity.getBody(), req, resp);
        }
    }

    private void writeBody(Object body, HttpServletRequest req, HttpServletResponse resp) {
        if (resp.getContentType() == null) {
            HttpMessageConverter best = converterRegistry.findBestConverter(body.getClass(), req.getHeader("Accept"));

            MediaType selected = best == null ? MediaType.APPLICATION_JSON : best.getSupportedMediaTypes().stream()
                    .filter(m -> !m.equals(MediaType.ALL))
                    .findFirst()
                    .orElse(MediaType.APPLICATION_JSON);

            resp.setContentType(selected + ";charset=UTF-8");
        }
        converterRegistry.write(body, body.getClass(), resp);
    }
}
