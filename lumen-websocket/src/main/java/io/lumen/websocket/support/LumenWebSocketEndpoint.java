package io.lumen.websocket.support;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.websocket.BinaryMessage;
import io.lumen.websocket.CloseStatus;
import io.lumen.websocket.TextMessage;
import io.lumen.websocket.WebSocketHandler;
import io.lumen.websocket.WebSocketSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

import java.nio.ByteBuffer;
import java.util.Map;

public class LumenWebSocketEndpoint extends jakarta.websocket.Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(LumenWebSocketEndpoint.class);

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private final WebSocketHandler handler;
    private Runnable setupSecurity;
    private Runnable cleanupSecurity;

    public LumenWebSocketEndpoint(WebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        Map<String, Object> props = config.getUserProperties();
        setupSecurity = (Runnable) props.get("lumen.security.setup");
        cleanupSecurity = (Runnable) props.get("lumen.security.cleanup");

        WebSocketSession ws = new WebSocketSessionAdapter(session);

        // Use the explicit Class<T> overload so Tomcat doesn't need to infer
        // the generic type via reflection (cast lambdas lose <T> at runtime).
        session.addMessageHandler(String.class, message -> {
            try {
                withSecurity(() -> handler.handleTextMessage(ws, new TextMessage(message)));
            } catch (Exception e) {
                logger.error("Error handling text message: {}", e.getMessage(), e);
                handler.handleError(ws, e);
            }
        });

        session.addMessageHandler(ByteBuffer.class, data -> {
            try {
                withSecurity(() -> handler.handleBinaryMessage(ws, new BinaryMessage(data.array())));
            } catch (Exception e) {
                logger.error("Error handling binary message: {}", e.getMessage(), e);
                handler.handleError(ws, e);
            }
        });

        try {
            withSecurity(() -> handler.afterConnectionEstablished(ws));
        } catch (Exception e) {
            logger.error("Error in afterConnectionEstablished: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        WebSocketSession ws = new WebSocketSessionAdapter(session);
        CloseStatus status = new CloseStatus(
                closeReason.getCloseCode().getCode(),
                closeReason.getReasonPhrase()
        );
        try {
            withSecurity(() -> handler.afterConnectionClosed(ws, status));
        } catch (Exception e) {
            logger.error("Error in afterConnectionClosed: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        handler.handleError(new WebSocketSessionAdapter(session), throwable);
    }

    private void withSecurity(ThrowingRunnable action) throws Exception {
        if (setupSecurity != null) setupSecurity.run();
        try {
            action.run();
        } finally {
            if (cleanupSecurity != null) cleanupSecurity.run();
        }
    }
}