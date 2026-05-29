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
}