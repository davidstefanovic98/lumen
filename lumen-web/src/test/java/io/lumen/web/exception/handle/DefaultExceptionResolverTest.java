package io.lumen.web.exception.handle;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.lumen.web.exception.TypeConversionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultExceptionResolverTest {

    private final DefaultExceptionResolver resolver = new DefaultExceptionResolver();

    private Logger logbackLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void captureLogs() {
        logbackLogger = (Logger) LoggerFactory.getLogger(DefaultExceptionResolver.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logbackLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachAppender() {
        logbackLogger.detachAppender(logAppender);
    }

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

    @Test
    void resolve_writerFails_doesNotPropagateAndStatusIsStillSet() throws Exception {
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        var response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenThrow(new java.io.IOException("client disconnected"));

        boolean handled = resolver.resolve(request, response, new IllegalStateException("boom"));

        // A failure to write the body must not escape resolve() as an unhandled exception,
        // and the status set before the write attempt must not be rolled back.
        assertTrue(handled);
        verify(response).setStatus(500);
    }

    @Test
    void resolve_writerFails_logsInsteadOfSwallowingSilently() throws Exception {
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        var response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenThrow(new java.io.IOException("client disconnected"));

        resolver.resolve(request, response, new IllegalStateException("boom"));

        assertTrue(logAppender.list.stream().anyMatch(e ->
                        e.getLevel() == Level.WARN && e.getThrowableProxy() != null
                                && "client disconnected".equals(e.getThrowableProxy().getMessage())),
                "a write failure must be logged, not silently discarded");
    }
}