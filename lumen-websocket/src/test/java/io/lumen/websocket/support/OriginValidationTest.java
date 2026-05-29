package io.lumen.websocket.support;

import io.lumen.websocket.AbstractWebSocketHandler;
import io.lumen.websocket.CloseStatus;
import io.lumen.websocket.TextMessage;
import io.lumen.websocket.WebSocketSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OriginValidationTest {

    static class NoopHandler extends AbstractWebSocketHandler {
        @Override public void afterConnectionEstablished(WebSocketSession s) {}
        @Override public void handleTextMessage(WebSocketSession s, TextMessage m) {}
        @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {}
    }

    private OriginValidatingConfigurator configurator(List<String> origins) {
        return new OriginValidatingConfigurator(new NoopHandler(), origins);
    }

    @Test
    void emptyOrigins_alwaysTrue_sameOriginAllowed() {
        // Empty list = same-origin only; checkOrigin returns true to let Tomcat proceed.
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
}
