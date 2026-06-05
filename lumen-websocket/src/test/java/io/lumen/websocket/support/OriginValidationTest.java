package io.lumen.websocket.support;

import io.lumen.websocket.AbstractWebSocketHandler;
import io.lumen.websocket.CloseStatus;
import io.lumen.websocket.HandshakeInterceptor;
import io.lumen.websocket.TextMessage;
import io.lumen.websocket.WebSocketSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OriginValidationTest {

    static class NoopHandler extends AbstractWebSocketHandler {
        @Override public void afterConnectionEstablished(WebSocketSession s) {}
        @Override public void handleTextMessage(WebSocketSession s, TextMessage m) {}
        @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {}
    }

    private OriginValidatingConfigurator configurator(List<String> origins) {
        return new OriginValidatingConfigurator(new NoopHandler(), origins, List.of());
    }

    private OriginValidatingConfigurator configurator(List<String> origins,
                                                       List<HandshakeInterceptor> interceptors) {
        return new OriginValidatingConfigurator(new NoopHandler(), origins, interceptors);
    }

    @Test
    void emptyOrigins_alwaysTrue_sameOriginAllowed() {
        assertTrue(configurator(List.of()).checkOrigin("http://app.example.com"));
    }

    @Test
    void wildcardOrigin_allowsAll() {
        assertTrue(configurator(List.of("*")).checkOrigin("http://evil.com"));
        assertTrue(configurator(List.of("*")).checkOrigin("http://any.origin.io"));
    }

    @Test
    void explicitOrigin_allowsMatchingOrigin() {
        assertTrue(configurator(List.of("http://app.example.com"))
                .checkOrigin("http://app.example.com"));
    }

    @Test
    void explicitOrigin_rejectsNonMatchingOrigin() {
        assertFalse(configurator(List.of("http://app.example.com"))
                .checkOrigin("http://evil.com"));
    }

    @Test
    void multipleExplicitOrigins_allowsAnyOfThem() {
        List<String> origins = List.of("http://app.example.com", "http://staging.example.com");
        assertTrue(configurator(origins).checkOrigin("http://app.example.com"));
        assertTrue(configurator(origins).checkOrigin("http://staging.example.com"));
        assertFalse(configurator(origins).checkOrigin("http://other.com"));
    }

    @Test
    void modifyHandshake_callsAllInterceptors() {
        Map<String, Object> captured = new HashMap<>();

        HandshakeInterceptor interceptorA = (req, props) -> props.put("a", "valueA");
        HandshakeInterceptor interceptorB = (req, props) -> props.put("b", "valueB");

        ServerEndpointConfig config = mock(ServerEndpointConfig.class);
        when(config.getUserProperties()).thenReturn(captured);
        HandshakeRequest request = mock(HandshakeRequest.class);
        HandshakeResponse response = mock(HandshakeResponse.class);

        configurator(List.of(), List.of(interceptorA, interceptorB))
                .modifyHandshake(config, request, response);

        assertEquals("valueA", captured.get("a"));
        assertEquals("valueB", captured.get("b"));
    }

    @Test
    void modifyHandshake_noInterceptors_doesNothing() {
        ServerEndpointConfig config = mock(ServerEndpointConfig.class);
        when(config.getUserProperties()).thenReturn(new HashMap<>());

        // Should not throw even with no interceptors.
        assertDoesNotThrow(() ->
                configurator(List.of()).modifyHandshake(config,
                        mock(HandshakeRequest.class), mock(HandshakeResponse.class)));
    }
}