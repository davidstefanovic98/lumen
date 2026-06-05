package io.lumen.websocket;

import jakarta.websocket.server.HandshakeRequest;

import java.util.Map;

/**
 * SPI called during the WebSocket handshake, before the connection is established.
 * Implementations can read HTTP request headers and query parameters and store
 * data in {@code userProperties}, which is then accessible via
 * {@link WebSocketSession#getAttributes()} and {@link WebSocketSession#getPrincipal()}.
 *
 * <p>Register as a light ({@code @Component}) or as an external instance in a
 * {@code LumenModule}. All registered interceptors are applied in container order.
 *
 * <p>{@code lumen-security} ships {@code SecurityHandshakeInterceptor}, which captures
 * the current {@code Authentication} and makes it available for the lifetime of the session.
 */
@FunctionalInterface
public interface HandshakeInterceptor {

    void beforeHandshake(HandshakeRequest request, Map<String, Object> userProperties);
}