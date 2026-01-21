package io.lumen.web.exception.handle;

import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public class CompositeExceptionResolver {
    private final List<ExceptionResolver> resolvers;

    public CompositeExceptionResolver(ControllerAdviceRegistry adviceRegistry, HttpMessageConverterRegistry converterRegistry) {
        // Order matters for resolvers
        this.resolvers = List.of(
                new GlobalExceptionHandleResolver(adviceRegistry, converterRegistry),
                new NotFoundExceptionResolver(),
                new MediaTypeExceptionResolver(),
                new DefaultExceptionResolver()
        );
    }

    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        for (ExceptionResolver resolver : resolvers) {
            if (resolver.resolve(req, resp, ex)) {
                return true;
            }
        }
        return false;
    }
}
