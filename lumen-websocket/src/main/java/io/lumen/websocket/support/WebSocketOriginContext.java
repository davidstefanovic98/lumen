package io.lumen.websocket.support;

/**
 * Carries the current request's own {@code scheme://host[:port]} from
 * {@link WebSocketOriginCaptureFilter} to {@link OriginValidatingConfigurator#checkOrigin},
 * which runs later on the same thread (see {@link WebSocketOriginCaptureFilter} for why this
 * indirection is necessary at all).
 */
final class WebSocketOriginContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private WebSocketOriginContext() {}

    static void set(String origin) {
        CURRENT.set(origin);
    }

    static String get() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }
}