package io.lumen.web.context;

import io.lumen.context.AnnotationApplicationContext;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.logging.StartupBanner;
import io.lumen.web.RouteRegistry;
import io.lumen.web.DispatcherServlet;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;

public class AnnotationWebApplicationContext implements WebApplicationContext {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationWebApplicationContext.class);
    private final AnnotationApplicationContext context;
    private final RouteRegistry routeRegistry;
    private final ControllerAdviceRegistry controllerAdviceRegistry;
    private final int port;
    private WebServer webServer;

    public AnnotationWebApplicationContext(Class<?> configClass, int port) {
        this.context = new AnnotationApplicationContext(configClass);
        logger.info("Initializing Lumen Web Application Context");
        this.port = port;
        logger.debug("Configuration class: {}", configClass.getName());

        this.routeRegistry = new RouteRegistry();
        this.controllerAdviceRegistry = new ControllerAdviceRegistry();
        ControllerScanner.scanControllers(context.getLightContainer().getLights().values(), routeRegistry);
        GlobalExceptionHandlerScanner.scanForControllerAdvice(
                context.getLightContainer().getLights().values(),
                controllerAdviceRegistry
        );
        logger.info("Application context initialized successfully");
    }

    @Override
    public void startWebServer() {
        try {
            StartupBanner.print(logger);

            logger.info("Starting web server on port {}", port);

            webServer = new WebServer(port);
            webServer.addServlet("dispatcher", new DispatcherServlet(routeRegistry, controllerAdviceRegistry));

            webServer.start();

            logger.info("Lumen application started successfully");
            logger.info("Server is running at http://localhost:{}", port);
        } catch (Exception e) {
            logger.error("Failed to start web server", e);
            throw new RuntimeException("Web server startup failed", e);
        }
    }

    public AnnotationApplicationContext getApplicationContext() {
        return context;
    }

    public RouteRegistry getRouteRegistry() {
        return routeRegistry;
    }

    public ControllerAdviceRegistry getControllerAdviceRegistry() {
        return controllerAdviceRegistry;
    }

    @Override
    public void stop() {
        if (webServer != null) {
            logger.info("Stopping web server...");
            webServer.stop();
            logger.info("Web server stopped");
        }
    }
}

