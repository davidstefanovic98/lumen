package io.lumen.security.websocket;

import io.lumen.security.authentication.Authentication;
import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.authority.SimpleGrantedAuthority;
import io.lumen.security.context.SecurityContextHolder;
import jakarta.websocket.server.HandshakeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SecurityHandshakeInterceptorTest {

    private final SecurityHandshakeInterceptor interceptor = new SecurityHandshakeInterceptor();

    @AfterEach
    void clear() {
        SecurityContextHolder.clear();
    }

    private Authentication authenticate(String user, String... roles) {
        var authorities = List.of(roles).stream()
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    private Map<String, Object> runHandshake() {
        Map<String, Object> props = new HashMap<>();
        interceptor.beforeHandshake(mock(HandshakeRequest.class), props);
        return props;
    }

    @Test
    void storesPrincipalInUserProperties() {
        Authentication auth = authenticate("alice", "USER");

        Map<String, Object> props = runHandshake();

        assertSame(auth, props.get(SecurityHandshakeInterceptor.PRINCIPAL_KEY));
    }

    @Test
    void nullPrincipal_whenNoAuthenticationPresent() {
        Map<String, Object> props = runHandshake();

        assertNull(props.get(SecurityHandshakeInterceptor.PRINCIPAL_KEY));
    }

    @Test
    void setupRunnable_restoresSecurityContextOnWorkerThread() throws Exception {
        authenticate("alice", "USER");
        Map<String, Object> props = runHandshake();

        // Simulate worker thread: clear context, then run setup.
        SecurityContextHolder.clear();
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "should start empty on worker thread");

        Runnable setup = (Runnable) props.get(SecurityHandshakeInterceptor.SETUP_KEY);
        setup.run();

        Authentication restored = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(restored);
        assertEquals("alice", restored.getName());
    }

    @Test
    void cleanupRunnable_clearsContextAfterHandlerCall() {
        authenticate("alice", "USER");
        Map<String, Object> props = runHandshake();

        Runnable setup = (Runnable) props.get(SecurityHandshakeInterceptor.SETUP_KEY);
        Runnable cleanup = (Runnable) props.get(SecurityHandshakeInterceptor.CLEANUP_KEY);

        setup.run();
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        cleanup.run();
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "context must be cleared after handler call");
    }

    @Test
    void capturesAtHandshakeTime_laterContextChangeHasNoEffect() {
        authenticate("alice", "USER");
        Map<String, Object> props = runHandshake();

        // Change the calling thread's context after the handshake.
        authenticate("bob", "ADMIN");

        Runnable setup = (Runnable) props.get(SecurityHandshakeInterceptor.SETUP_KEY);
        SecurityContextHolder.clear();
        setup.run();

        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName(),
                "setup must restore the auth captured at handshake time, not the current context");
    }
}