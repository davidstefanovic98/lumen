package io.lumen.websocket.support;

import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.websocket.WebSocketHandler;
import io.lumen.websocket.annotation.LumenWebSocket;

public class WebSocketHandlerProcessor implements LightProcessor {

    private final WebSocketHandlerRegistry registry;

    public WebSocketHandlerProcessor(WebSocketHandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        if (!(instance instanceof WebSocketHandler handler)) return instance;

        Class<?> type = instance.getClass();
        LumenWebSocket ann = findAnnotation(type);
        if (ann != null) {
            registry.register(ann.value(), handler, ann.allowedOrigins());
        }
        return instance;
    }

    private LumenWebSocket findAnnotation(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            LumenWebSocket ann = current.getAnnotation(LumenWebSocket.class);
            if (ann != null) return ann;
            current = current.getSuperclass();
        }
        return null;
    }
}