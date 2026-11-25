package io.lumen.web.exception.handle;

import io.lumen.web.handler.ResponseBodyWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

public class CompositeExceptionResolver {
    private final List<ExceptionResolver> resolvers;

    public CompositeExceptionResolver(ControllerAdviceRegistry adviceRegistry, ResponseBodyWriter bodyWriter) {
        ErrorResponseWriter errorWriter = new ErrorResponseWriter(bodyWriter);
        List<ExceptionResolver> list = new ArrayList<>();
        list.add(new GlobalExceptionHandleResolver(adviceRegistry, bodyWriter));

        if (isValidationPresent()) {
            list.add(new ValidationExceptionResolver(errorWriter));
        }

        list.add(new DefaultExceptionResolver(errorWriter));
        this.resolvers = list;
    }

    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        for (ExceptionResolver resolver : resolvers) {
            if (resolver.resolve(req, resp, ex)) {
                return true;
            }
        }
        return false;
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