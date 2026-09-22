package io.lumen.websocket.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketOriginCaptureFilterTest {

    private final WebSocketOriginCaptureFilter filter = new WebSocketOriginCaptureFilter();

    @AfterEach
    void clear() {
        WebSocketOriginContext.clear();
    }

    private HttpServletRequest requestFor(String scheme, String host, int port) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn(scheme);
        when(request.getServerName()).thenReturn(host);
        when(request.getServerPort()).thenReturn(port);
        return request;
    }

    @Test
    void capturesOrigin_omittingDefaultHttpPort() throws Exception {
        HttpServletRequest request = requestFor("http", "app.example.com", 80);
        FilterChain chain = (req, res) -> assertEquals("http://app.example.com", WebSocketOriginContext.get());

        filter.doFilter(request, mock(jakarta.servlet.ServletResponse.class), chain);
    }

    @Test
    void capturesOrigin_omittingDefaultHttpsPort() throws Exception {
        HttpServletRequest request = requestFor("https", "app.example.com", 443);
        FilterChain chain = (req, res) -> assertEquals("https://app.example.com", WebSocketOriginContext.get());

        filter.doFilter(request, mock(jakarta.servlet.ServletResponse.class), chain);
    }

    @Test
    void capturesOrigin_includingNonDefaultPort() throws Exception {
        HttpServletRequest request = requestFor("http", "localhost", 8080);
        FilterChain chain = (req, res) -> assertEquals("http://localhost:8080", WebSocketOriginContext.get());

        filter.doFilter(request, mock(jakarta.servlet.ServletResponse.class), chain);
    }

    @Test
    void clearsContextAfterChainCompletes() throws Exception {
        HttpServletRequest request = requestFor("http", "app.example.com", 80);
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, mock(jakarta.servlet.ServletResponse.class), chain);

        assertNull(WebSocketOriginContext.get());
    }

    @Test
    void clearsContextEvenWhenChainThrows() {
        HttpServletRequest request = requestFor("http", "app.example.com", 80);
        FilterChain chain = (req, res) -> { throw new RuntimeException("boom"); };

        assertThrows(RuntimeException.class,
                () -> filter.doFilter(request, mock(jakarta.servlet.ServletResponse.class), chain));

        assertNull(WebSocketOriginContext.get());
    }
}