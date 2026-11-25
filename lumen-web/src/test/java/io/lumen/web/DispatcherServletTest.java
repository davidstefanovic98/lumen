package io.lumen.web;

import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.exception.handle.CompositeExceptionResolver;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.handler.ResponseBodyWriter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.resource.ResourceProvider;
import io.lumen.web.resource.StaticResourceResultHandler;
import io.lumen.web.support.CapturingOutputStream;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DispatcherServletTest {

    DispatcherServlet servlet;
    RouteRegistry registry;
    RouteInvoker invoker;
    ResourceProvider resourceProvider;
    HttpServletRequest request;
    HttpServletResponse response;
    CapturingOutputStream responseBody;

    static class FakeController {
        public void handle() {}
    }

    @BeforeEach
    void setUp() throws Exception {
        registry = new RouteRegistry();
        HttpMessageConverterRegistry converterRegistry = new HttpMessageConverterRegistry();
        ControllerAdviceRegistry adviceRegistry = new ControllerAdviceRegistry();
        ResponseBodyWriter bodyWriter = new ResponseBodyWriter(converterRegistry);
        invoker = spy(new RouteInvoker(bodyWriter, new CompositeMethodArgumentResolver()));
        resourceProvider = mock(ResourceProvider.class);
        when(resourceProvider.getResource(anyString())).thenReturn(null);
        when(resourceProvider.getWelcomePage()).thenReturn(null);

        servlet = new DispatcherServlet(
                registry,
                new CompositeExceptionResolver(adviceRegistry, bodyWriter),
                invoker,
                resourceProvider,
                new StaticResourceResultHandler()
        );

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getContextPath()).thenReturn("");
        responseBody = new CapturingOutputStream();
        when(response.getOutputStream()).thenReturn(responseBody);
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

    // --- ASYNC re-dispatch of a CompletableFuture handler ---

    @Test
    void asyncDispatchWithStoredOutcome_resumesInsteadOfReinvokingTheRoute() throws Exception {
        registerRoute("GET", "/ping");
        when(request.getRequestURI()).thenReturn("/ping");
        when(request.getMethod()).thenReturn("GET");
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
        doReturn(true).when(invoker).resumeAsync(request, response);

        servlet.service(request, response);

        verify(invoker).resumeAsync(request, response);
        verify(invoker, never()).invokeAndWrite(any(), any(), any());
    }

    @Test
    void asyncDispatchWithStoredFailure_goesThroughTheSameExceptionPathAsSync() throws Exception {
        when(request.getRequestURI()).thenReturn("/ping");
        when(request.getMethod()).thenReturn("GET");
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
        doThrow(new IllegalStateException("async-boom")).when(invoker).resumeAsync(request, response);

        servlet.service(request, response);

        verify(response).setStatus(500);
        assertEquals("{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"async-boom\"}",
                responseBody.body());
    }

    @Test
    void asyncDispatchWithoutStoredOutcome_isRoutedNormally() throws Exception {
        registerRoute("GET", "/ping");
        when(request.getRequestURI()).thenReturn("/ping");
        when(request.getMethod()).thenReturn("GET");
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
        doNothing().when(invoker).invokeAndWrite(any(), any(), any());

        servlet.service(request, response);

        verify(invoker).invokeAndWrite(any(RouteMatch.class), eq(request), eq(response));
    }

    @Test
    void requestDispatch_neverConsultsAsyncOutcome() throws Exception {
        registerRoute("GET", "/ping");
        when(request.getRequestURI()).thenReturn("/ping");
        when(request.getMethod()).thenReturn("GET");
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        doNothing().when(invoker).invokeAndWrite(any(), any(), any());

        servlet.service(request, response);

        verify(invoker, never()).resumeAsync(any(), any());
    }

    @Test
    void noRoute_noResource_sends500WithNotFoundError() throws Exception {
        when(request.getRequestURI()).thenReturn("/nonexistent");
        when(request.getMethod()).thenReturn("GET");

        servlet.service(request, response);

        // NotFoundException carries @ResponseStatus(NOT_FOUND) → 404 via DefaultExceptionResolver
        verify(response).setStatus(404);
        assertEquals("{\"status\":404,\"error\":\"Not Found\",\"message\":\"No route or static resource found for GET /nonexistent\"}",
                responseBody.body());
    }

    private void registerProducingRoute(String pattern, String produces) throws Exception {
        Object controller = new FakeController();
        Method method = FakeController.class.getMethod("handle");
        Route route = new Route(controller, method, "GET", pattern);
        route.setProduces(new String[]{produces});
        registry.register(route);
    }

    @Test
    void wildcardAccept_doesNotCause406() throws Exception {
        registerProducingRoute("/api/data", "application/json");
        when(request.getRequestURI()).thenReturn("/api/data");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Accept")).thenReturn("*/*");
        doNothing().when(invoker).invokeAndWrite(any(), any(), any());

        servlet.service(request, response);

        verify(invoker).invokeAndWrite(any(RouteMatch.class), eq(request), eq(response));
    }

    @Test
    void browserAcceptHeader_doesNotCause406() throws Exception {
        registerProducingRoute("/api/items", "application/json");
        when(request.getRequestURI()).thenReturn("/api/items");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Accept")).thenReturn(
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        doNothing().when(invoker).invokeAndWrite(any(), any(), any());

        servlet.service(request, response);

        verify(invoker).invokeAndWrite(any(RouteMatch.class), eq(request), eq(response));
    }

    @Test
    void subtypeWildcardAccept_doesNotCause406() throws Exception {
        registerProducingRoute("/api/text", "text/plain");
        when(request.getRequestURI()).thenReturn("/api/text");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Accept")).thenReturn("text/*");
        doNothing().when(invoker).invokeAndWrite(any(), any(), any());

        servlet.service(request, response);

        verify(invoker).invokeAndWrite(any(RouteMatch.class), eq(request), eq(response));
    }

    @Test
    void incompatibleAcceptHeader_causes406() throws Exception {
        registerProducingRoute("/api/strict", "application/json");
        when(request.getRequestURI()).thenReturn("/api/strict");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Accept")).thenReturn("text/html");

        servlet.service(request, response);

        verify(response).setStatus(406);
        assertEquals("{\"status\":406,\"error\":\"Not Acceptable\",\"message\":\"Supported: application/json\"}",
                responseBody.body());
    }

    @Test
    void unsupportedContentType_causes415() throws Exception {
        Object controller = new FakeController();
        Route route = new Route(controller, FakeController.class.getMethod("handle"), "POST", "/api/upload");
        route.setConsumes(new String[]{"application/json"});
        registry.register(route);
        when(request.getRequestURI()).thenReturn("/api/upload");
        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn("text/plain");

        servlet.service(request, response);

        verify(response).setStatus(415);
        assertEquals("{\"status\":415,\"error\":\"Unsupported Media Type\",\"message\":\"Expected: application/json\"}",
                responseBody.body());
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