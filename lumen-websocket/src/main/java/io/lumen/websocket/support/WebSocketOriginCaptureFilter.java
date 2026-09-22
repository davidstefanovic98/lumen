package io.lumen.websocket.support;

import io.lumen.web.filter.LumenFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Captures this request's own {@code scheme://host[:port]} into {@link WebSocketOriginContext},
 * so {@link OriginValidatingConfigurator#checkOrigin} can do a genuine same-origin comparison
 * when a {@code @LumenWebSocket} endpoint has no explicit {@code allowedOrigins} configured.
 *
 * <p>This exists because {@code ServerEndpointConfig.Configurator#checkOrigin(String)} only ever
 * receives the {@code Origin} header value — it has no access to the request's own host, and the
 * JSR-356 spec's own container default doesn't enforce same-origin either (Tomcat's
 * {@code DefaultServerEndpointConfigurator.checkOrigin()} unconditionally returns {@code true}).
 * {@code HandshakeRequest}, which does carry the request URI, is only passed to
 * {@code modifyHandshake()}, which runs after {@code checkOrigin()} has already decided whether
 * to proceed, with no clean way to reject a handshake from there.
 *
 * <p>A WebSocket upgrade request passes through Lumen's own filter chain before Tomcat's
 * {@code WsFilter} performs the actual handshake, the same way it already does for
 * {@code SecurityContextHolder} population (see {@code SecurityHandshakeInterceptor}) — verified
 * empirically with a real embedded Tomcat: a filter registered the same way production registers
 * it (before {@code WsSci} is added to the servlet context) runs and completes, on the same
 * thread, before {@code checkOrigin()}/{@code modifyHandshake()} are invoked. Capturing the
 * expected origin here and reading it back in {@code checkOrigin()} is therefore the only way to
 * get real same-origin enforcement within this API.
 */
public class WebSocketOriginCaptureFilter implements LumenFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest http) {
            WebSocketOriginContext.set(sameOrigin(http));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            WebSocketOriginContext.clear();
        }
    }

    /**
     * Builds the value this server's own origin would have in a browser {@code Origin} header —
     * which omits the port for the scheme's default port (80 for http, 443 for https).
     */
    private static String sameOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }

    @Override
    public int getOrder() {
        return -200; // early — needs to be set before anything downstream might need it
    }
}