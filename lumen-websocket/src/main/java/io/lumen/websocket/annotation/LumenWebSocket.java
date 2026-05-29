package io.lumen.websocket.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LumenWebSocket {
    String value(); // URL path, e.g. "/ws/chat"

    // Allowed origins for the WebSocket handshake.
    // Empty means same-origin only (the host of the request).
    // Use "*" to allow all origins (development only).
    String[] allowedOrigins() default {};
}