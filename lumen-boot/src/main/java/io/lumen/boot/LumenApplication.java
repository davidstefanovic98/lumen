package io.lumen.boot;

import io.lumen.web.context.AnnotationWebApplicationContext;

public final class LumenApplication {

    private LumenApplication() {}

    public static void run(Class<?> configClass, String[] args) {
        try {
            new AnnotationWebApplicationContext(configClass).startWebServer();
        } catch (Exception e) {
            // Already logged with full context by startWebServer() - exit without letting the
            // JVM's default uncaught-exception handler print a third, redundant stack trace.
            System.exit(1);
        }
    }
}
