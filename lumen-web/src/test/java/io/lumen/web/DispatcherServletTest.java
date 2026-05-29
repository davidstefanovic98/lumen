package io.lumen.web;

import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.resource.ResourceProvider;
import io.lumen.web.resource.StaticResourceResultHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

class DispatcherServletTest {

    DispatcherServlet servlet;
    RouteRegistry registry;
    RouteInvoker invoker;
    ResourceProvider resourceProvider;
    HttpServletRequest request;
    HttpServletResponse response;

    static class FakeController {
        public void handle() {}
    }

    @BeforeEach
    void setUp() throws Exception {
        registry = new RouteRegistry();
        invoker = spy(new RouteInvoker(new HttpMessageConverterRegistry(), new CompositeMethodArgumentResolver()));
        resourceProvider = mock(ResourceProvider.class);
        when(resourceProvider.getResource(anyString())).thenReturn(null);
        when(resourceProvider.getWelcomePage()).thenReturn(null);

        servlet = new DispatcherServlet(
                registry,
                new ControllerAdviceRegistry(),
                new HttpMessageConverterRegistry(),
                invoker,
                resourceProvider,
                new StaticResourceResultHandler()
        );

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getContextPath()).thenReturn("");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    private void registerRoute(String httpMethod, String pattern) throws Exception {
        Object controller = new FakeController();
        Method method = FakeController.class.getMethod("handle");
        registry.register(new Route(controller, method, httpMethod, pattern));
    }

    @Test
    void matchedRoute_delegatesToInvoker() throws Exception {
        registerRoute("GET", "/ping");
        when(request.getRequestURI()).thenReturn("/ping");
        when(request.getMethod()).thenReturn("GET");

        // invoker.invokeAndWrite is package-private — spy will call real impl which returns 204 on void
        doNothing().when(invoker).invokeAndWrite(any(), any(), any());

        servlet.service(request, response);

        verify(invoker).invokeAndWrite(any(RouteMatch.class), eq(request), eq(response));
    }

    @Test
    void noRoute_noResource_sends500WithNotFoundError() throws Exception {
        when(request.getRequestURI()).thenReturn("/nonexistent");
        when(request.getMethod()).thenReturn("GET");

        servlet.service(request, response);

        // NotFoundException is handled by NotFoundExceptionResolver → 404
        verify(response).setStatus(404);
    }

    @Test
    void trailingSlashStripped_routeStillMatches() throws Exception {
        registerRoute("GET", "/users");
        when(request.getRequestURI()).thenReturn("/users/");
        when(request.getMethod()).thenReturn("GET");
        doNothing().when(invoker).invokeAndWrite(any(), any(), any());

        servlet.service(request, response);

        verify(invoker).invokeAndWrite(any(RouteMatch.class), eq(request), eq(response));
    }

}