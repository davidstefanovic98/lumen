package io.lumen.websocket.support;

import io.lumen.websocket.AbstractWebSocketHandler;
import io.lumen.websocket.CloseStatus;
import io.lumen.websocket.TextMessage;
import io.lumen.websocket.WebSocketSession;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real-server regression test for the WebSocket same-origin fix: proves that
 * WebSocketOriginCaptureFilter + OriginValidatingConfigurator together actually enforce
 * same-origin when no allowedOrigins are configured, using a genuine embedded Tomcat rather than
 * mocks — the SCI/filter registration order this depends on (Lumen's own SCI, hence its filters,
 * registered before WsSci) can't be verified with mocks alone.
 */
class WebSocketOriginEnforcementIntegrationTest {

    private Tomcat tomcat;
    private int port;

    @BeforeEach
    void startServer() throws Exception {
        tomcat = new Tomcat();
        File baseDir = new File(System.getProperty("java.io.tmpdir"), "lumen-ws-origin-test-" + System.nanoTime());
        tomcat.setBaseDir(baseDir.getAbsolutePath());
        tomcat.setPort(0);
        tomcat.getConnector();
        Context ctx = tomcat.addContext("", baseDir.getAbsolutePath());

        // A catch-all servlet, exactly like production's DispatcherServlet mapped to /*, so
        // requests actually reach the Wrapper/filter-chain pipeline.
        Tomcat.addServlet(ctx, "fallback", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        });
        ctx.addServletMappingDecoded("/*", "fallback");

        // 1) Lumen's own filter, added first — same order as AnnotationWebApplicationContext.
        ctx.addServletContainerInitializer((classes, servletContext) -> {
            FilterRegistration.Dynamic reg = servletContext.addFilter("originCapture", new WebSocketOriginCaptureFilter());
            reg.setAsyncSupported(true);
            reg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        }, null);

        // 2) WsSci, added second — same order as tryRegisterWebSocketSCI().
        Class<?> wsciClass = Class.forName("org.apache.tomcat.websocket.server.WsSci");
        ctx.addServletContainerInitializer(
                (ServletContainerInitializer) wsciClass.getDeclaredConstructor().newInstance(), null);

        // 3) The @LumenWebSocket endpoint itself, registered the same way WebSocketInitializer does.
        ctx.addServletContainerInitializer((classes, servletContext) -> {
            ServerContainer sc = (ServerContainer) servletContext.getAttribute(ServerContainer.class.getName());
            try {
                sc.addEndpoint(ServerEndpointConfig.Builder
                        .create(LumenWebSocketEndpoint.class, "/echo")
                        .configurator(new OriginValidatingConfigurator(new NoopHandler(), List.of(), List.of()))
                        .build());
            } catch (DeploymentException e) {
                throw new RuntimeException(e);
            }
        }, null);

        tomcat.start();
        port = tomcat.getConnector().getLocalPort();
    }

    @AfterEach
    void stopServer() throws LifecycleException {
        tomcat.stop();
        tomcat.destroy();
    }

    @Test
    void sameOriginRequest_connectsSuccessfully() throws Exception {
        assertTrue(connect(originHeader("http", "localhost", port)),
                "a WebSocket request whose Origin matches this server's own origin must connect");
    }

    @Test
    void crossOriginRequest_isRejected() {
        // Regression: this used to always succeed regardless of Origin, when allowedOrigins
        // wasn't configured for the endpoint.
        assertThrows(DeploymentException.class, () -> connect(originHeader("http", "evil.com", 1234)),
                "a WebSocket request from a different origin must be rejected when " +
                "allowedOrigins isn't configured");
    }

    private boolean connect(String originHeaderValue) throws Exception {
        WebSocketContainer client = ContainerProvider.getWebSocketContainer();
        CountDownLatch opened = new CountDownLatch(1);

        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(Map<String, List<String>> headers) {
                        headers.put("Origin", List.of(originHeaderValue));
                    }
                })
                .build();

        Session session = client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session s, EndpointConfig c) {
                opened.countDown();
            }
        }, config, URI.create("ws://localhost:" + port + "/echo"));

        boolean didOpen = opened.await(5, TimeUnit.SECONDS);
        session.close();
        return didOpen;
    }

    private static String originHeader(String scheme, String host, int port) {
        return scheme + "://" + host + ":" + port;
    }

    static class NoopHandler extends AbstractWebSocketHandler {
        @Override public void afterConnectionEstablished(WebSocketSession session) {}
        @Override public void handleTextMessage(WebSocketSession session, TextMessage message) {}
        @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {}
    }
}