package io.lumen.websocket.support;

import io.lumen.websocket.WebSocketHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WebSocketHandlerRegistry {

    public record Registration(WebSocketHandler handler, List<String> allowedOrigins) {}

    private final List<String> globalAllowedOrigins;
    private final Map<String, Registration> registrations = new LinkedHashMap<>();

    public WebSocketHandlerRegistry(List<String> globalAllowedOrigins) {
        this.globalAllowedOrigins = globalAllowedOrigins;
    }

    public void register(String path, WebSocketHandler handler, String[] annotationOrigins) {
        // Annotation origins take priority; fall back to global if annotation didn't declare any
        List<String> effective = annotationOrigins.length > 0
                ? Arrays.asList(annotationOrigins)
                : globalAllowedOrigins;
        registrations.put(path, new Registration(handler, effective));
    }

    public Map<String, Registration> getRegistrations() {
        return Collections.unmodifiableMap(registrations);
    }
}