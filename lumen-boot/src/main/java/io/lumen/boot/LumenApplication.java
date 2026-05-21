package io.lumen.boot;

import io.lumen.web.context.AnnotationWebApplicationContext;

public final class LumenApplication {

    private LumenApplication() {}

    public static void run(Class<?> configClass, String[] args) {
        new AnnotationWebApplicationContext(configClass).startWebServer();
    }
}
