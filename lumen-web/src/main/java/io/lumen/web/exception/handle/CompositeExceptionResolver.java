package io.lumen.web.exception.handle;

import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

public class CompositeExceptionResolver {
    private final List<ExceptionResolver> resolvers;

    public CompositeExceptionResolver(ControllerAdviceRegistry adviceRegistry, HttpMessageConverterRegistry converterRegistry) {
        List<ExceptionResolver> list = new ArrayList<>();
        list.add(new GlobalExceptionHandleResolver(adviceRegistry, converterRegistry));
        list.add(new NotFoundExceptionResolver());
        list.add(new MediaTypeExceptionResolver());

        if (isValidationPresent()) {
            list.add(new ValidationExceptionResolver());
        }

        list.add(new DefaultExceptionResolver());
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