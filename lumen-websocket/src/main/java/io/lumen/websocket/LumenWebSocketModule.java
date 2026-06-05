package io.lumen.websocket;

import io.lumen.context.PackageScanner;
import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.websocket.annotation.LumenWebSocket;
import io.lumen.websocket.support.WebSocketHandlerProcessor;
import io.lumen.websocket.support.WebSocketHandlerRegistry;
import io.lumen.websocket.support.WebSocketInitializer;

import java.util.Arrays;
import java.util.List;

@Order(4)
public class LumenWebSocketModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        List<String> globalOrigins = resolveGlobalOrigins(container);

        WebSocketHandlerRegistry registry = new WebSocketHandlerRegistry(globalOrigins);
        registry.setInterceptorsSupplier(() -> container.getLights(HandshakeInterceptor.class));
        container.registerExternalInstance(WebSocketHandlerRegistry.class, registry);

        container.addPostProcessor(new WebSocketHandlerProcessor(registry));

        PackageScanner.scan(basePackages).forEach(clazz -> {
            if (clazz.isAnnotationPresent(LumenWebSocket.class)
                    && WebSocketHandler.class.isAssignableFrom(clazz)) {
                container.register(clazz);
            }
        });

        container.register(WebSocketInitializer.class);
    }

    private List<String> resolveGlobalOrigins(LightContainer container) {
        if (!container.hasLight(Environment.class))
            return List.of();

        Environment env = container.getLight(Environment.class);
        String property = env.getProperty("lumen.websocket.allowed-origins");
        if (property == null || property.isBlank())
            return List.of();

        return Arrays.stream(property.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}