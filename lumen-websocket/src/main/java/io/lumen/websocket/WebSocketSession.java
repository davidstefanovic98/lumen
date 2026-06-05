package io.lumen.websocket;

import java.io.IOException;
import java.util.Map;

public interface WebSocketSession {

    String getId();

    boolean isOpen();

    void sendText(String message) throws IOException;

    void sendBinary(byte[] data) throws IOException;

    void close() throws IOException;

    void close(CloseStatus status) throws IOException;

    Map<String, String> getPathParameters();

    /**
     * Returns the principal (authentication) associated with this session,
     * or {@code null} if the connection was made without authentication.
     * Cast to {@code io.lumen.security.authentication.Authentication} when
     * {@code lumen-security} is on the classpath.
     */
    Object getPrincipal();

    /**
     * Returns the mutable attributes map for this session. Contains any values
     * stored by {@link HandshakeInterceptor}s during the handshake.
     */
    Map<String, Object> getAttributes();
}