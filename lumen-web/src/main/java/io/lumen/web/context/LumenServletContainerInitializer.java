package io.lumen.web.context;

import io.lumen.core.logging.LoggingConfigurator;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.util.Set;

public class LumenServletContainerInitializer implements ServletContainerInitializer {

    private final AnnotationWebApplicationContext lumenContext;

    public LumenServletContainerInitializer(AnnotationWebApplicationContext lumenContext) {
        this.lumenContext = lumenContext;
    }

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        LoggingConfigurator.configure();
        lumenContext.onWebStartup(ctx);
    }
}
