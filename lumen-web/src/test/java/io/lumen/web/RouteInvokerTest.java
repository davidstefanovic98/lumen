package io.lumen.web;

import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.handler.ResponseBodyWriter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
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
        ResponseBodyWriter bodyWriter = new ResponseBodyWriter(converterRegistry);
        invoker = new RouteInvoker(bodyWriter, new CompositeMethodArgumentResolver());
        controller = new TestController();

        response = mock(HttpServletResponse.class);

        // Request attributes are real, so the stored async outcome survives into resumeAsync()
        request = mock(HttpServletRequest.class);
        Map<String, Object> attributes = new HashMap<>();
        doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(inv -> attributes.remove(inv.<String>getArgument(0)))
                .when(request).removeAttribute(anyString());
        when(request.getAttribute(anyString())).thenAnswer(inv -> attributes.get(inv.<String>getArgument(0)));
        when(request.getContextPath()).thenReturn("");

        asyncContext = mock(AsyncContext.class);
        when(asyncContext.getRequest()).thenReturn(request);
        when(asyncContext.getResponse()).thenReturn(response);
        when(request.startAsync()).thenReturn(asyncContext);
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
    // The future's outcome is not written from the completing thread: it is stored on the request
    // and the request is re-dispatched, so DispatcherServlet handles it through the normal path.

    @Test
    void completableFuture_startsAsyncAndRedispatchesOnCompletion() throws Exception {
        invoker.invokeAndWrite(matchFor("futureReturnsValue"), request, response);

        verify(request).startAsync();
        // whenComplete runs synchronously for already-completed futures
        verify(asyncContext).dispatch();
        verify(asyncContext, never()).complete();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void completableFuture_failure_isRedispatchedNotResolvedOnTheCompletingThread() throws Exception {
        invoker.invokeAndWrite(matchFor("futureThrows"), request, response);

        verify(asyncContext).dispatch();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void resumeAsync_failure_rethrowsTheOriginalExceptionUnwrapped() throws Exception {
        invoker.invokeAndWrite(matchFor("futureThrows"), request, response);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> invoker.resumeAsync(request, response));
        assertEquals("async-boom", ex.getMessage());
    }

    @Test
    void resumeAsync_nullResult_sets204() throws Exception {
        invoker.invokeAndWrite(matchFor("futureReturnsNull"), request, response);

        assertTrue(invoker.resumeAsync(request, response));
        verify(response).setStatus(204);
    }

    @Test
    void resumeAsync_consumesTheOutcome_soItIsHandledOnlyOnce() throws Exception {
        invoker.invokeAndWrite(matchFor("futureReturnsNull"), request, response);

        assertTrue(invoker.resumeAsync(request, response));
        assertFalse(invoker.resumeAsync(request, response));
    }

    @Test
    void resumeAsync_withoutStoredOutcome_returnsFalse() throws Exception {
        assertFalse(invoker.resumeAsync(request, response));
    }
}
