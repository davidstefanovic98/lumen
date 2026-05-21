package io.lumen.web.context;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;

import java.util.Set;

public class LumenServletContainerInitializer implements ServletContainerInitializer {

    private final AnnotationWebApplicationContext lumenContext;

    public LumenServletContainerInitializer(AnnotationWebApplicationContext lumenContext) {
        this.lumenContext = lumenContext;
    }

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) {
        lumenContext.onWebStartup(ctx);
    }
}
