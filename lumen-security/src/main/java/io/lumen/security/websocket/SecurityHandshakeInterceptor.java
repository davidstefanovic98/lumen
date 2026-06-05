package io.lumen.security.websocket;

import io.lumen.security.authentication.Authentication;
import io.lumen.security.context.DefaultSecurityContext;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.websocket.HandshakeInterceptor;
import jakarta.websocket.server.HandshakeRequest;

import java.util.Map;

/**
 * Captures the caller's {@link Authentication} during the WebSocket handshake and
 * makes it available for the lifetime of the session.
 *
 * <p>Stored in user properties under {@value #PRINCIPAL_KEY}; accessible via
 * {@code WebSocketSession.getPrincipal()}.
 *
 * <p>Also stores setup/cleanup {@link Runnable}s under {@value #SETUP_KEY} /
 * {@value #CLEANUP_KEY}. {@code LumenWebSocketEndpoint} calls these around every
 * handler invocation so that {@code SecurityContextHolder} is populated on the
 * WebSocket thread — enabling {@code @PreAuthorize} and direct
 * {@code SecurityContextHolder.getContext()} calls inside handler methods.
 */
public class SecurityHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PRINCIPAL_KEY = "lumen.security.principal";
    public static final String SETUP_KEY = "lumen.security.setup";
    public static final String CLEANUP_KEY = "lumen.security.cleanup";

    @Override
    public void beforeHandshake(HandshakeRequest request, Map<String, Object> userProperties) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        userProperties.put(PRINCIPAL_KEY, auth);

        userProperties.put(SETUP_KEY, (Runnable) () -> {
            DefaultSecurityContext ctx = new DefaultSecurityContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
        });

        userProperties.put(CLEANUP_KEY, (Runnable) SecurityContextHolder::clear);
    }
}