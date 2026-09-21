package io.lumen.web.handler;

import io.lumen.web.Route;
import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.http.HttpStatus;
import io.lumen.web.http.ResponseEntity;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestResultHandlerTest {

    private final RestResultHandler handler = new RestResultHandler(new HttpMessageConverterRegistry());

    static class TestController {
        public String plain() { return "hello"; }

        @ResponseStatus(HttpStatus.CREATED)
        public String created() { return "hello"; }

        @ResponseStatus(HttpStatus.CREATED)
        public ResponseEntity<String> createdButReturnsEntity() {
            return ResponseEntity.status(200).body("hello");
        }
    }

    private Route routeFor(String methodName) throws Exception {
        Method m = TestController.class.getMethod(methodName);
        Route route = new Route(new TestController(), m, "GET", "/test");
        route.setRest(true);
        return route;
    }

    private HttpServletResponse mockResponse() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        return resp;
    }

    @Test
    void handle_noResponseStatusAnnotation_defaultsTo200() throws Exception {
        Route route = routeFor("plain");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mockResponse();

        handler.handle("hello", new Object[0], route, req, resp);

        verify(resp).setStatus(200);
    }

    @Test
    void handle_methodLevelResponseStatus_usesAnnotatedStatus() throws Exception {
        Route route = routeFor("created");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mockResponse();

        handler.handle("hello", new Object[0], route, req, resp);

        verify(resp).setStatus(201);
    }

    @Test
    void handle_responseEntityResult_ignoresMethodLevelResponseStatus() throws Exception {
        Route route = routeFor("createdButReturnsEntity");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mockResponse();

        handler.handle(ResponseEntity.status(200).body("hello"), new Object[0], route, req, resp);

        // ResponseEntity carries its own status explicitly, which always wins over @ResponseStatus
        verify(resp).setStatus(200);
        verify(resp, never()).setStatus(201);
    }
}