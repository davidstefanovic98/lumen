package io.lumen.websocket.support;

import io.lumen.websocket.WebSocketHandler;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.List;

class OriginValidatingConfigurator extends ServerEndpointConfig.Configurator {

    private final WebSocketHandler handler;
    private final List<String> allowedOrigins;

    OriginValidatingConfigurator(WebSocketHandler handler, List<String> allowedOrigins) {
        this.handler = handler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) {
        return endpointClass.cast(new LumenWebSocketEndpoint(handler));
    }

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        if (allowedOrigins.isEmpty()) {
            // No explicit origins — same-origin only (default secure behaviour)
            return true;
        }
        if (allowedOrigins.contains("*")) {
            return true;
        }
        return allowedOrigins.contains(originHeaderValue);
    }
}