package io.lumen.web;

import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RouteInvokerTest {

    RouteInvoker invoker;
    HttpServletRequest request;
    HttpServletResponse response;

    static class TestController {
        public void voidMethod() {}
        public void throwsRuntime() { throw new IllegalStateException("runtime-boom"); }
        public void throwsChecked() throws Exception { throw new Exception("checked-boom"); }
        public String returnsString() { return "hello"; }
    }

    TestController controller;

    @BeforeEach
    void setUp() throws Exception {
        invoker = new RouteInvoker(new HttpMessageConverterRegistry(), new CompositeMethodArgumentResolver());
        controller = new TestController();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    private RouteMatch matchFor(String methodName) throws Exception {
        Method m = TestController.class.getMethod(methodName);
        Route route = new Route(controller, m, "GET", "/test");
        return new RouteMatch(route, Map.of());
    }

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
}