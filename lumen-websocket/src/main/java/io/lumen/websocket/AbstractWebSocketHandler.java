package io.lumen.websocket;

public abstract class AbstractWebSocketHandler implements WebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {}

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {}

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {}

    @Override
    public void handleError(WebSocketSession session, Throwable error) {}

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {}
}