package io.lumen.web;

import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.exception.handle.CompositeExceptionResolver;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RouteInvokerTest {

    RouteInvoker invoker;
    HttpServletRequest request;
    HttpServletResponse response;

    static class TestController {
        public void voidMethod() {}
        public void throwsRuntime()        { throw new IllegalStateException("runtime-boom"); }
        public void throwsChecked() throws Exception { throw new Exception("checked-boom"); }
        public String returnsString()      { return "hello"; }

        public CompletableFuture<String> futureReturnsValue() {
            return CompletableFuture.completedFuture("async-result");
        }
        public CompletableFuture<String> futureThrows() {
            return CompletableFuture.failedFuture(new IllegalStateException("async-boom"));
        }
        public CompletableFuture<Void> futureReturnsNull() {
            return CompletableFuture.completedFuture(null);
        }
    }

    TestController controller;
    AsyncContext asyncContext;

    @BeforeEach
    void setUp() throws Exception {
        HttpMessageConverterRegistry converterRegistry = new HttpMessageConverterRegistry();
        ControllerAdviceRegistry adviceRegistry = new ControllerAdviceRegistry();
        invoker = new RouteInvoker(converterRegistry, new CompositeMethodArgumentResolver(),
                new CompositeExceptionResolver(adviceRegistry, converterRegistry));
        controller = new TestController();

        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        asyncContext = mock(AsyncContext.class);
        when(asyncContext.getRequest()).thenReturn(mock(HttpServletRequest.class));
        when(asyncContext.getResponse()).thenReturn(response);

        request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.startAsync()).thenReturn(asyncContext);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    private RouteMatch matchFor(String methodName) throws Exception {
        Method m = TestController.class.getMethod(methodName);
        Route route = new Route(controller, m, "GET", "/test");
        return new RouteMatch(route, Map.of());
    }

    // --- synchronous ---

    @Test
    void voidMethod_sets204() throws Exception {
        invoker.invokeAndWrite(matchFor("voidMethod"), request, response);
        verify(response).setStatus(204);
    }

    @Test
    void runtimeException_propagatesDirectly() throws Exception {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> invoker.invokeAndWrite(matchFor("throwsRuntime"), request, response));
        assertEquals("runtime-boom", ex.getMessage());
    }

    @Test
    void checkedException_unwrappedFromInvocationTargetException() {
        Exception ex = assertThrows(Exception.class,
                () -> invoker.invokeAndWrite(matchFor("throwsChecked"), request, response));
        assertEquals("checked-boom", ex.getMessage());
        assertFalse(ex instanceof java.lang.reflect.InvocationTargetException,
                "ITE must be unwrapped — caller should see the original checked exception");
    }

    // --- CompletableFuture ---

    @Test
    void completableFuture_callsStartAsync() throws Exception {
        invoker.invokeAndWrite(matchFor("futureReturnsValue"), request, response);
        verify(request).startAsync();
    }

    @Test
    void completableFuture_completesAsyncContext_onSuccess() throws Exception {
        invoker.invokeAndWrite(matchFor("futureReturnsValue"), request, response);
        // whenComplete runs synchronously for already-completed futures
        verify(asyncContext).complete();
    }

    @Test
    void completableFuture_completesAsyncContext_onFailure() throws Exception {
        invoker.invokeAndWrite(matchFor("futureThrows"), request, response);
        verify(asyncContext).complete();
    }

    @Test
    void completableFuture_nullResult_sets204() throws Exception {
        invoker.invokeAndWrite(matchFor("futureReturnsNull"), request, response);
        verify(response).setStatus(204);
        verify(asyncContext).complete();
    }

}