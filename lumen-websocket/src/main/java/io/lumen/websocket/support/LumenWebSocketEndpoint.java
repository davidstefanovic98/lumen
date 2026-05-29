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
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

import java.nio.ByteBuffer;

public class LumenWebSocketEndpoint extends jakarta.websocket.Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(LumenWebSocketEndpoint.class);

    private final WebSocketHandler handler;

    public LumenWebSocketEndpoint(WebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        WebSocketSession ws = new WebSocketSessionAdapter(session);

        session.addMessageHandler((MessageHandler.Whole<String>) message -> {
            try {
                handler.handleTextMessage(ws, new TextMessage(message));
            } catch (Exception e) {
                logger.error("Error handling text message: {}", e.getMessage(), e);
                handler.handleError(ws, e);
            }
        });

        session.addMessageHandler((MessageHandler.Whole<ByteBuffer>) data -> {
            try {
                handler.handleBinaryMessage(ws, new BinaryMessage(data.array()));
            } catch (Exception e) {
                logger.error("Error handling binary message: {}", e.getMessage(), e);
                handler.handleError(ws, e);
            }
        });

        try {
            handler.afterConnectionEstablished(ws);
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
            handler.afterConnectionClosed(ws, status);
        } catch (Exception e) {
            logger.error("Error in afterConnectionClosed: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        handler.handleError(new WebSocketSessionAdapter(session), throwable);
    }
}