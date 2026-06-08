package io.lumen.websocket.support;

import io.lumen.core.DeferredLumenInitializer;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketInitializerTest {

    @Test
    void webSocketInitializer_implementsDeferredLumenInitializer() {
        assertTrue(DeferredLumenInitializer.class.isAssignableFrom(WebSocketInitializer.class),
                "WebSocketInitializer must implement DeferredLumenInitializer so it runs in phase 3, " +
                "after WsSci has populated ServerContainer");
    }

    @Test
    void onStartup_whenServerContainerMissing_logsWarningAndDoesNotThrow() {
        WebSocketHandlerRegistry registry = new WebSocketHandlerRegistry(List.of());
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getAttribute(anyString())).thenReturn(null);

        WebSocketInitializer initializer = new WebSocketInitializer(registry, servletContext);
        assertDoesNotThrow(initializer::onStartup);
    }
}