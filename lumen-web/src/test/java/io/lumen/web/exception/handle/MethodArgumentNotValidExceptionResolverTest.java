package io.lumen.web.exception.handle;

import io.lumen.web.exception.MethodArgumentNotValidException;
import io.lumen.web.http.ErrorResponse.FieldError;
import io.lumen.web.handler.ResponseBodyWriter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.support.CapturingOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MethodArgumentNotValidExceptionResolverTest {

    private final MethodArgumentNotValidExceptionResolver resolver = new MethodArgumentNotValidExceptionResolver(
            new ErrorResponseWriter(new ResponseBodyWriter(new HttpMessageConverterRegistry())));

    private HttpServletRequest request;
    private HttpServletResponse response;
    private CapturingOutputStream body;

    @BeforeEach
    void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        body = new CapturingOutputStream();
        when(response.getOutputStream()).thenReturn(body);
    }

    @Test
    void resolve_methodArgumentNotValid_writes422WithEscapedViolations() {
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(List.of(
                new FieldError("name", "must not be blank"),
                new FieldError("title", "must not contain \"quotes\"")), null);

        boolean handled = resolver.resolve(request, response, ex);

        assertTrue(handled);
        verify(response).setStatus(422);
        JsonNode json = new ObjectMapper().readTree(body.body());
        assertEquals("Unprocessable Entity", json.get("error").asString());
        assertEquals("Validation failed with 2 violation(s)", json.get("message").asString());
        assertEquals("name", json.get("violations").get(0).get("field").asString());
        assertEquals("must not contain \"quotes\"", json.get("violations").get(1).get("message").asString());
    }

    @Test
    void resolve_methodArgumentNotValidAsCause_isFound() {
        MethodArgumentNotValidException cause = new MethodArgumentNotValidException(
                List.of(new FieldError("name", "required")), null);

        assertTrue(resolver.resolve(request, response, new RuntimeException("wrapped", cause)));
        verify(response).setStatus(422);
    }

    @Test
    void resolve_unrelatedException_returnsFalseAndWritesNothing() {
        assertFalse(resolver.resolve(request, response, new IllegalStateException("boom")));
        verify(response, never()).setStatus(anyInt());
    }
}