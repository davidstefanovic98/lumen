package io.lumen.web.exception.handle;

import io.lumen.web.exception.TypeConversionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultExceptionResolverTest {

    private final DefaultExceptionResolver resolver = new DefaultExceptionResolver();

    @Test
    void resolve_responseStatusAnnotatedException_usesAnnotatedStatus() throws Exception {
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        boolean handled = resolver.resolve(request, response,
                new TypeConversionException("Failed to convert value 'abc' to type int",
                        new NumberFormatException("abc")));

        assertTrue(handled);
        verify(response).setStatus(400);
    }

    @Test
    void resolve_unannotatedException_defaultsTo500() throws Exception {
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        var response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        boolean handled = resolver.resolve(request, response, new IllegalStateException("boom"));

        assertTrue(handled);
        verify(response).setStatus(500);
    }
}