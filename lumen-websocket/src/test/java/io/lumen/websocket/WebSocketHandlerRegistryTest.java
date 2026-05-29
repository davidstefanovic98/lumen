package io.lumen.websocket;

import io.lumen.websocket.annotation.LumenWebSocket;
import io.lumen.websocket.support.WebSocketHandlerProcessor;
import io.lumen.websocket.support.WebSocketHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketHandlerRegistryTest {

    WebSocketHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WebSocketHandlerRegistry(List.of());
    }

    @LumenWebSocket("/ws/chat")
    static class ChatHandler extends AbstractWebSocketHandler {
        @Override public void afterConnectionEstablished(WebSocketSession s) {}
        @Override public void handleTextMessage(WebSocketSession s, TextMessage m) {}
        @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {}
    }

    @LumenWebSocket(value = "/ws/events", allowedOrigins = {"http://app.example.com"})
    static class EventsHandler extends AbstractWebSocketHandler {
        @Override public void afterConnectionEstablished(WebSocketSession s) {}
        @Override public void handleTextMessage(WebSocketSession s, TextMessage m) {}
        @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {}
    }

    @LumenWebSocket(value = "/ws/all", allowedOrigins = {"*"})
    static class AllOriginsHandler extends AbstractWebSocketHandler {
        @Override public void afterConnectionEstablished(WebSocketSession s) {}
        @Override public void handleTextMessage(WebSocketSession s, TextMessage m) {}
        @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {}
    }

    @Test
    void register_storesHandlerAtPath() {
        ChatHandler handler = new ChatHandler();
        registry.register("/ws/chat", handler, new String[0]);
        assertNotNull(registry.getRegistrations().get("/ws/chat"));
        assertSame(handler, registry.getRegistrations().get("/ws/chat").handler());
    }

    @Test
    void noAnnotationOrigins_fallsBackToGlobalOrigins() {
        WebSocketHandlerRegistry reg = new WebSocketHandlerRegistry(List.of("http://global.example.com"));
        reg.register("/ws/chat", new ChatHandler(), new String[0]);
        assertEquals(List.of("http://global.example.com"),
                reg.getRegistrations().get("/ws/chat").allowedOrigins());
    }

    @Test
    void annotationOrigins_takePriorityOverGlobal() {
        WebSocketHandlerRegistry reg = new WebSocketHandlerRegistry(List.of("http://global.example.com"));
        reg.register("/ws/events", new EventsHandler(), new String[]{"http://app.example.com"});
        assertEquals(List.of("http://app.example.com"),
                reg.getRegistrations().get("/ws/events").allowedOrigins());
    }

    @Test
    void wildcardOrigin_stored() {
        registry.register("/ws/all", new AllOriginsHandler(), new String[]{"*"});
        assertEquals(List.of("*"), registry.getRegistrations().get("/ws/all").allowedOrigins());
    }

    @Test
    void registrationsMap_isUnmodifiable() {
        registry.register("/ws/chat", new ChatHandler(), new String[0]);
        assertThrows(UnsupportedOperationException.class,
                () -> registry.getRegistrations().put("/ws/new", null));
    }

    // --- WebSocketHandlerProcessor ---

    @Test
    void processor_registersAnnotatedHandler() {
        WebSocketHandlerRegistry reg = new WebSocketHandlerRegistry(List.of());
        WebSocketHandlerProcessor processor = new WebSocketHandlerProcessor(reg);

        ChatHandler handler = new ChatHandler();
        processor.afterInstantiation(null, handler);

        assertTrue(reg.getRegistrations().containsKey("/ws/chat"));
    }

    @Test
    void processor_ignoresNonHandlerBeans() {
        WebSocketHandlerRegistry reg = new WebSocketHandlerRegistry(List.of());
        WebSocketHandlerProcessor processor = new WebSocketHandlerProcessor(reg);

        processor.afterInstantiation(null, "not a handler");

        assertTrue(reg.getRegistrations().isEmpty());
    }

    @Test
    void processor_handlerWithoutAnnotation_notRegistered() {
        WebSocketHandlerRegistry reg = new WebSocketHandlerRegistry(List.of());
        WebSocketHandlerProcessor processor = new WebSocketHandlerProcessor(reg);

        // A WebSocketHandler with no @LumenWebSocket annotation
        WebSocketHandler bare = new AbstractWebSocketHandler() {
            @Override public void afterConnectionEstablished(WebSocketSession s) {}
            @Override public void handleTextMessage(WebSocketSession s, TextMessage m) {}
            @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {}
        };

        processor.afterInstantiation(null, bare);

        assertTrue(reg.getRegistrations().isEmpty());
    }
}