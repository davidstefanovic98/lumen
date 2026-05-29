package io.lumen.websocket;

public interface WebSocketHandler {

    void afterConnectionEstablished(WebSocketSession session) throws Exception;

    void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception;

    default void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {}

    default void handleError(WebSocketSession session, Throwable error) {}

    void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception;
}