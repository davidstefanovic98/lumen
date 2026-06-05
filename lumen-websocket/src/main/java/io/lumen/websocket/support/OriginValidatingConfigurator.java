package io.lumen.websocket.support;

import io.lumen.websocket.HandshakeInterceptor;
import io.lumen.websocket.WebSocketHandler;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.List;

class OriginValidatingConfigurator extends ServerEndpointConfig.Configurator {

    private final WebSocketHandler handler;
    private final List<String> allowedOrigins;
    private final List<HandshakeInterceptor> interceptors;

    OriginValidatingConfigurator(WebSocketHandler handler, List<String> allowedOrigins,
                                 List<HandshakeInterceptor> interceptors) {
        this.handler = handler;
        this.allowedOrigins = allowedOrigins;
        this.interceptors = interceptors;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request,
                                HandshakeResponse response) {
        for (HandshakeInterceptor interceptor : interceptors) {
            interceptor.beforeHandshake(request, config.getUserProperties());
        }
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