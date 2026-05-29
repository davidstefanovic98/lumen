package io.lumen.websocket.support;

import io.lumen.websocket.CloseStatus;
import io.lumen.websocket.WebSocketSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

class WebSocketSessionAdapter implements WebSocketSession {

    private final Session session;

    WebSocketSessionAdapter(Session session) {
        this.session = session;
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public void sendText(String message) throws IOException {
        session.getBasicRemote().sendText(message);
    }

    @Override
    public void sendBinary(byte[] data) throws IOException {
        session.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
    }

    @Override
    public void close() throws IOException {
        session.close();
    }

    @Override
    public void close(CloseStatus status) throws IOException {
        session.close(new CloseReason(
                CloseReason.CloseCodes.getCloseCode(status.getCode()),
                status.getReason()
        ));
    }

    @Override
    public Map<String, String> getPathParameters() {
        return session.getPathParameters();
    }

    Session unwrap() {
        return session;
    }
}