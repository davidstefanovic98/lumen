package io.lumen.web.exception.handle;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.lumen.web.exception.NotFoundException;
import io.lumen.web.exception.TypeConversionException;
import io.lumen.web.handler.ResponseBodyWriter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.support.CapturingOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultExceptionResolverTest {

    /** Inherits @ResponseStatus(NOT_FOUND) from NotFoundException. */
    static class OrderNotFoundException extends NotFoundException {
        OrderNotFoundException(String message) { super(message); }
    }

    private final DefaultExceptionResolver resolver = new DefaultExceptionResolver(
            new ErrorResponseWriter(new ResponseBodyWriter(new HttpMessageConverterRegistry())));

    private HttpServletRequest request;
    private HttpServletResponse response;
    private CapturingOutputStream body;

    private Logger logbackLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        response = mock(HttpServletResponse.class);
        body = new CapturingOutputStream();
        when(response.getOutputStream()).thenReturn(body);

        logbackLogger = (Logger) LoggerFactory.getLogger(ErrorResponseWriter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logbackLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachAppender() {
        logbackLogger.detachAppender(logAppender);
    }

    @Test
    void resolve_responseStatusAnnotatedException_usesAnnotatedStatus() {
        boolean handled = resolver.resolve(request, response,
                new TypeConversionException("Failed to convert value 'abc' to type int",
                        new NumberFormatException("abc")));

        assertTrue(handled);
        verify(response).setStatus(400);
    }

    @Test
    void resolve_unannotatedException_defaultsTo500() {
        boolean handled = resolver.resolve(request, response, new IllegalStateException("boom"));

        assertTrue(handled);
        verify(response).setStatus(500);
        assertEquals("{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"boom\"}", body.body());
    }

    @Test
    void resolve_subclassOfAnnotatedException_inheritsStatus() {
        resolver.resolve(request, response, new OrderNotFoundException("order-42"));

        // @ResponseStatus is @Inherited - a user subtype of NotFoundException must not fall back to 500
        verify(response).setStatus(404);
        verify(response, never()).setStatus(500);
    }

    @Test
    void resolve_messageWithJsonSpecialCharacters_producesValidEscapedJson() {
        String message = "bad \"quoted\" value \\ with\nnewline";

        resolver.resolve(request, response, new IllegalStateException(message));

        // Hand-built JSON used to break on a '"' in the message; Jackson escapes it properly
        JsonNode json = new ObjectMapper().readTree(body.body());
        assertEquals(500, json.get("status").asInt());
        assertEquals(message, json.get("message").asString());
    }

    @Test
    void resolve_nullMessage_fallsBackToReasonPhrase() {
        resolver.resolve(request, response, new IllegalStateException());

        assertEquals("{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Internal Server Error\"}",
                body.body());
    }

    @Test
    void resolve_writeFails_doesNotPropagateAndStatusIsStillSet() throws Exception {
        when(response.getOutputStream()).thenThrow(new java.io.IOException("client disconnected"));

        boolean handled = resolver.resolve(request, response, new IllegalStateException("boom"));

        // A failure to write the body must not escape resolve() as an unhandled exception,
        // and the status set before the write attempt must not be rolled back.
        assertTrue(handled);
        verify(response).setStatus(500);
    }

    @Test
    void resolve_writeFails_logsInsteadOfSwallowingSilently() throws Exception {
        when(response.getOutputStream()).thenThrow(new java.io.IOException("client disconnected"));

        resolver.resolve(request, response, new IllegalStateException("boom"));

        assertTrue(logAppender.list.stream().anyMatch(e -> e.getLevel() == Level.WARN && e.getThrowableProxy() != null),
                "a write failure must be logged, not silently discarded");
    }
}