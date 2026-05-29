package io.lumen.websocket.support;

import io.lumen.core.LumenInitializer;
import io.lumen.core.annotation.Order;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.websocket.WebSocketHandler;
import jakarta.servlet.ServletContext;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.List;
import java.util.Map;

@Order(2)
public class WebSocketInitializer implements LumenInitializer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketInitializer.class);

    private final WebSocketHandlerRegistry registry;
    private final ServletContext servletContext;

    public WebSocketInitializer(WebSocketHandlerRegistry registry, ServletContext servletContext) {
        this.registry = registry;
        this.servletContext = servletContext;
    }

    @Override
    public void onStartup() {
        ServerContainer serverContainer = (ServerContainer)
                servletContext.getAttribute(ServerContainer.class.getName());

        if (serverContainer == null) {
            logger.warn("WebSocket ServerContainer not found — WebSocket endpoints will not be registered. " +
                    "Ensure tomcat-embed-websocket is on the classpath.");
            return;
        }

        for (Map.Entry<String, WebSocketHandlerRegistry.Registration> entry : registry.getRegistrations().entrySet()) {
            String path = entry.getKey();
            WebSocketHandler handler = entry.getValue().handler();
            List<String> allowedOrigins = entry.getValue().allowedOrigins();

            try {
                ServerEndpointConfig config = ServerEndpointConfig.Builder
                        .create(LumenWebSocketEndpoint.class, path)
                        .configurator(new OriginValidatingConfigurator(handler, allowedOrigins))
                        .build();

                serverContainer.addEndpoint(config);
                logger.info("WebSocket endpoint registered: {} → {} (origins: {})",
                        path, handler.getClass().getSimpleName(),
                        allowedOrigins.isEmpty() ? "same-origin" : allowedOrigins);
            } catch (Exception e) {
                logger.error("Failed to register WebSocket endpoint {}: {}", path, e.getMessage(), e);
            }
        }
    }

}