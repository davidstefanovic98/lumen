package io.lumen.web.context;

import io.lumen.core.LumenInitializer;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.logging.LoggingConfigurator;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.List;

public class LumenContextInitializer implements ServletContextListener {
    private final AnnotationWebApplicationContext lumenContext;
    private static final Logger logger = LoggerFactory.getLogger(LumenContextInitializer.class);

    public LumenContextInitializer(AnnotationWebApplicationContext lumenContext) {
        this.lumenContext = lumenContext;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext actualContext = sce.getServletContext();

        LoggingConfigurator.configure();

        var container = lumenContext.getApplicationContext().getLightContainer();
        container.registerExternalInstance(ServletContext.class, actualContext);
        lumenContext.getApplicationContext().initialize();

        List<LumenInitializer> initializers = container.internals().getLightsByType(LumenInitializer.class);
        for (LumenInitializer initializer : initializers) {
            logger.info("Starting module: {}", initializer.getClass().getSimpleName());
            initializer.onStartup();
        }
        lumenContext.registerFilters(actualContext);
        lumenContext.refreshWebComponents();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            lumenContext.stop();
        } catch (Exception ignored) {
            // If the server failed to start, stopping it might throw a transition error
        }
    }
}
