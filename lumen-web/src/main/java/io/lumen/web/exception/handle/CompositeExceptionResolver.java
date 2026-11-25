package io.lumen.web.exception.handle;

import io.lumen.web.handler.ResponseBodyWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns an exception into an HTTP response. {@link #resolve} always writes one: the specific
 * resolvers are tried in order, and {@link DefaultExceptionResolver} terminates the chain
 * ({@code @ResponseStatus} on the exception type, otherwise a logged 500), so callers never
 * need an "unresolved" fallback of their own.
 */
public class CompositeExceptionResolver {
    private final List<ExceptionResolver> resolvers;
    private final DefaultExceptionResolver terminal;

    public CompositeExceptionResolver(ControllerAdviceRegistry adviceRegistry, ResponseBodyWriter bodyWriter) {
        ErrorResponseWriter errorWriter = new ErrorResponseWriter(bodyWriter);
        List<ExceptionResolver> list = new ArrayList<>();
        list.add(new GlobalExceptionHandleResolver(adviceRegistry, bodyWriter));

        if (isValidationPresent()) {
            list.add(new ValidationExceptionResolver(errorWriter));
        }

        this.resolvers = list;
        this.terminal = new DefaultExceptionResolver(errorWriter);
    }

    public void resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        for (ExceptionResolver resolver : resolvers) {
            if (resolver.resolve(req, resp, ex)) {
                return;
            }
        }
        terminal.resolve(req, resp, ex);
    }

    private boolean isValidationPresent() {
        try {
            Class.forName("io.lumen.validation.ValidationException");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}