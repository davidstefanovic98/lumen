package io.lumen.web.exception.handle;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.lumen.web.annotation.ExceptionHandler;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.http.ResponseEntity;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandleResolverTest {

    static class OrderNotFoundException extends RuntimeException {
        OrderNotFoundException(String message) { super(message); }
    }

    static class WorkingAdvice {
        @ExceptionHandler(OrderNotFoundException.class)
        ResponseEntity<String> handle(OrderNotFoundException ex) {
            return ResponseEntity.status(404).body(ex.getMessage());
        }
    }

    static class BrokenAdvice {
        @ExceptionHandler(OrderNotFoundException.class)
        ResponseEntity<String> handle(OrderNotFoundException ex) {
            throw new IllegalStateException("advice itself is broken");
        }
    }

    private final HttpMessageConverterRegistry converterRegistry = new HttpMessageConverterRegistry();

    private HttpServletRequest request;
    private HttpServletResponse response;

    private Logger logbackLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        logbackLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandleResolver.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logbackLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachAppender() {
        logbackLogger.detachAppender(logAppender);
    }

    @Test
    void resolve_matchingHandler_writesResponseEntityAndReturnsTrue() {
        ControllerAdviceRegistry registry = new ControllerAdviceRegistry();
        registry.registerAdvice(new WorkingAdvice());
        GlobalExceptionHandleResolver resolver = new GlobalExceptionHandleResolver(registry, converterRegistry);

        boolean handled = resolver.resolve(request, response, new OrderNotFoundException("order-42"));

        assertTrue(handled);
        verify(response).setStatus(404);
    }

    @Test
    void resolve_noMatchingHandler_returnsFalse() {
        ControllerAdviceRegistry registry = new ControllerAdviceRegistry();
        GlobalExceptionHandleResolver resolver = new GlobalExceptionHandleResolver(registry, converterRegistry);

        boolean handled = resolver.resolve(request, response, new OrderNotFoundException("order-42"));

        assertFalse(handled);
    }

    @Test
    void resolve_handlerMethodThrows_returnsFalseAndLogsInsteadOfSwallowingSilently() {
        ControllerAdviceRegistry registry = new ControllerAdviceRegistry();
        registry.registerAdvice(new BrokenAdvice());
        GlobalExceptionHandleResolver resolver = new GlobalExceptionHandleResolver(registry, converterRegistry);

        boolean handled = resolver.resolve(request, response, new OrderNotFoundException("order-42"));

        assertFalse(handled);
        assertTrue(logAppender.list.stream().anyMatch(e ->
                        e.getLevel() == Level.ERROR && e.getThrowableProxy() != null
                                && "advice itself is broken".equals(e.getThrowableProxy().getMessage())),
                "a secondary exception from an @ExceptionHandler method must be logged, not silently discarded");
    }
}