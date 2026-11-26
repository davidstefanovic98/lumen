package io.lumen.web.exception.handle;

import io.lumen.web.annotation.ExceptionHandler;
import io.lumen.web.handler.ResponseBodyWriter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.http.ResponseEntity;
import io.lumen.web.support.CapturingOutputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompositeExceptionResolverTest {

    /** Stands in for an exception a servlet filter owns, e.g. a security exception. */
    static class FilterOwnedException extends RuntimeException {
        FilterOwnedException(String message) { super(message); }
    }

    static class CheckedFilterOwnedException extends Exception {}

    static class FilterOwnedAdvice {
        @ExceptionHandler(FilterOwnedException.class)
        public ResponseEntity<String> handle(FilterOwnedException ex) {
            return ResponseEntity.status(409).body("handled-by-advice");
        }
    }

    private static final FilterChainExceptionMatcher MATCHER =
            ex -> ex instanceof FilterOwnedException || ex instanceof CheckedFilterOwnedException;

    private final ResponseBodyWriter bodyWriter = new ResponseBodyWriter(new HttpMessageConverterRegistry());
    private HttpServletRequest request;
    private HttpServletResponse response;
    private CapturingOutputStream body;

    @BeforeEach
    void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        response = mock(HttpServletResponse.class);
        body = new CapturingOutputStream();
        when(response.getOutputStream()).thenReturn(body);
    }

    private CompositeExceptionResolver resolver(Object... advices) {
        ControllerAdviceRegistry registry = new ControllerAdviceRegistry();
        for (Object advice : advices) registry.registerAdvice(advice);
        return new CompositeExceptionResolver(registry, bodyWriter, () -> List.of(MATCHER));
    }

    @Test
    void claimedException_withoutHandler_isRethrownUnwrittenForTheFilterChain() {
        FilterOwnedException ex = new FilterOwnedException("owned");

        FilterOwnedException thrown = assertThrows(FilterOwnedException.class,
                () -> resolver().resolve(request, response, ex));

        assertSame(ex, thrown, "the original exception must escape, not a wrapper");
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void claimedException_withMatchingExceptionHandler_isHandledByTheAdvice() throws Exception {
        resolver(new FilterOwnedAdvice()).resolve(request, response, new FilterOwnedException("owned"));

        // @ExceptionHandler runs before the filter-chain check - Spring MVC precedence
        verify(response).setStatus(409);
        assertEquals("handled-by-advice", body.body());
    }

    @Test
    void claimedExceptionInCauseChain_isRethrownAsTheOriginalException() {
        IllegalStateException wrapper = new IllegalStateException("wrapper", new FilterOwnedException("owned"));

        assertSame(wrapper, assertThrows(IllegalStateException.class,
                () -> resolver().resolve(request, response, wrapper)));
    }

    @Test
    void claimedCheckedException_isRethrownWrappedInServletException() {
        CheckedFilterOwnedException ex = new CheckedFilterOwnedException();

        ServletException thrown = assertThrows(ServletException.class,
                () -> resolver().resolve(request, response, ex));

        assertSame(ex, thrown.getCause());
    }

    @Test
    void unclaimedException_isWrittenByTheTerminalResolver() throws Exception {
        resolver().resolve(request, response, new IllegalStateException("boom"));

        verify(response).setStatus(500);
        assertEquals("{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"boom\"}", body.body());
    }

    @Test
    void methodArgumentNotValid_isWrittenAs422WithViolations_withNoValidationModuleOnTheClasspath() throws Exception {
        // lumen-web no longer depends on lumen-validation at all; this resolver must be wired
        // unconditionally rather than behind a classpath probe.
        resolver().resolve(request, response, new io.lumen.web.exception.MethodArgumentNotValidException(
                List.of(new io.lumen.web.http.ErrorResponse.FieldError("name", "must not be blank")), null));

        verify(response).setStatus(422);
        assertEquals("{\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Validation failed with 1 violation(s)\","
                + "\"violations\":[{\"field\":\"name\",\"message\":\"must not be blank\"}]}", body.body());
    }

    @Test
    void matchers_areCollectedLazilyAndOnlyWhenNeeded() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        CompositeExceptionResolver resolver = new CompositeExceptionResolver(
                new ControllerAdviceRegistry(), bodyWriter, () -> { lookups.incrementAndGet(); return List.of(); });

        assertEquals(0, lookups.get(), "matchers must not be collected at construction time");
        resolver.resolve(request, response, new IllegalStateException("boom"));
        assertEquals(1, lookups.get());
    }
}