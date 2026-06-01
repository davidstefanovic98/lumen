package io.lumen.web.context;

import io.lumen.context.AnnotationApplicationContext;
import io.lumen.core.LumenInitializer;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.DispatcherServlet;
import io.lumen.web.RouteInvoker;
import io.lumen.web.RouteRegistry;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.filter.LumenFilter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.multipart.MultipartConfig;
import io.lumen.web.resource.ResourceProvider;
import io.lumen.web.resource.StaticResourceResultHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;

import java.net.BindException;
import java.util.EnumSet;
import java.util.List;

public class AnnotationWebApplicationContext implements WebApplicationContext {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationWebApplicationContext.class);
    private final AnnotationApplicationContext context;
    private RouteRegistry routeRegistry;
    private final int port;
    private WebServer webServer;
    private volatile boolean started = false;

    public AnnotationWebApplicationContext(Class<?> configClass, int port) {
        this.port = port;
        this.context = new AnnotationApplicationContext();
        var container = context.getLightContainer();
        this.routeRegistry = container.internals().getLightByType(RouteRegistry.class);
        context.scan(configClass);
    }

    public AnnotationWebApplicationContext(Class<?> configClass) {
        this.port = -1;
        this.context = new AnnotationApplicationContext();
        var container = context.getLightContainer();
        this.routeRegistry = container.internals().getLightByType(RouteRegistry.class);
        context.scan(configClass);
    }

    @Override
    public void startWebServer() {
        if (started) {
            throw new IllegalStateException("startWebServer() has already been called on this context.");
        }

        int resolvedPort = this.port != -1 ? this.port
                : Integer.parseInt(context.getEnvironment().getProperty("server.port", "8080"));
        int shutdownTimeout = Integer.parseInt(
                context.getEnvironment().getProperty("lumen.shutdown.timeout-seconds", "30"));

        try {
            webServer = new WebServer(resolvedPort);
            tryRegisterWebSocketSCI(webServer);
            webServer.addSCI(new LumenServletContainerInitializer(this));
            webServer.start();
            started = true;

            int actualPort = resolvedPort == 0 ? webServer.getBoundPort() : resolvedPort;
            logger.info("Lumen application started successfully on port: {}", actualPort);

            registerShutdownHook(shutdownTimeout);
            webServer.await();

        } catch (Exception e) {
            Throwable root = rootCause(e);
            if (root instanceof BindException) {
                throw new RuntimeException(
                        "Port " + resolvedPort + " is already in use. " +
                        "Change server.port in application.properties or stop the process using that port.", e);
            }
            logger.error("Critical failure during web server startup", e);
            throw new RuntimeException("Failed to start web server on port " + resolvedPort, e);
        }
    }

    /**
     * This is called by the Initializer (SCI) while the ServletContext is still "unlocked".
     */
    public void onWebStartup(ServletContext servletContext) {
        logger.info("Initializing Web Context via SCI...");
        var container = context.getLightContainer();
        container.registerExternalInstance(ServletContext.class, servletContext);
        context.initialize();

        this.routeRegistry = container.internals().getLightByType(RouteRegistry.class);

        container.internals().getLightsByType(LumenInitializer.class)
                .forEach(LumenInitializer::onStartup);

        this.registerFilters(servletContext);
        this.registerDispatcherServlet(servletContext);

        logger.info("Web context initialized: {} routes registered", routeRegistry.getRouteCount());
    }

    @Override
    public void stop() {
        stop(0);
    }

    public void stop(int gracePeriodSeconds) {
        if (webServer != null) {
            logger.info("Lumen application shutting down (grace period: {}s)...", gracePeriodSeconds);
            webServer.stopGracefully(gracePeriodSeconds);
        }
        closeResources();
        logger.info("Lumen application stopped.");
    }

    private void registerShutdownHook(int timeoutSeconds) {
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                stop(timeoutSeconds), "lumen-shutdown"));
    }

    private void closeResources() {
        // Close EntityManagerFactory if JPA is on the classpath and an EMF was registered.
        // Using reflection to keep lumen-web free of a hard JPA dependency.
        var container = context.getLightContainer();
        try {
            @SuppressWarnings("unchecked")
            Class<Object> emfClass = (Class<Object>)
                    Class.forName("jakarta.persistence.EntityManagerFactory");
            if (container.hasLight(emfClass)) {
                Object emf = container.getLight(emfClass);
                emfClass.getMethod("close").invoke(emf);
                logger.info("EntityManagerFactory closed.");
            }
        } catch (ClassNotFoundException ignored) {
            // JPA not on classpath
        } catch (Exception e) {
            logger.warn("Error closing EntityManagerFactory: {}", e.getMessage());
        }
    }

    private void tryRegisterWebSocketSCI(WebServer webServer) {
        try {
            Class<?> wsciClass = Class.forName("org.apache.tomcat.websocket.server.WsSci");
            webServer.addSCI((ServletContainerInitializer) wsciClass.getDeclaredConstructor().newInstance());
            logger.info("WebSocket support enabled (WsSci registered)");
        } catch (ClassNotFoundException ignored) {
            // tomcat-embed-websocket not on classpath — WebSocket disabled
        } catch (Exception e) {
            logger.warn("Could not register WebSocket SCI: {}", e.getMessage());
        }
    }

    private void registerDispatcherServlet(ServletContext servletContext) {
        var container = context.getLightContainer();
        var internals = container.internals();
        container.registerExternalInstance(ResourceProvider.class, new ResourceProvider(servletContext));
        container.registerExternalInstance(StaticResourceResultHandler.class, new StaticResourceResultHandler());

        DispatcherServlet dispatcher = new DispatcherServlet(
                internals.getLightByType(RouteRegistry.class),
                internals.getLightByType(ControllerAdviceRegistry.class),
                internals.getLightByType(HttpMessageConverterRegistry.class),
                internals.getLightByType(RouteInvoker.class),
                internals.getLightByType(ResourceProvider.class),
                internals.getLightByType(StaticResourceResultHandler.class)
        );

        var registration = servletContext.addServlet("dispatcher", dispatcher);
        registration.addMapping("/*");

        var multipartConfig = container.internals().getLightByType(MultipartConfig.class);
        if (multipartConfig != null) {
            registration.setMultipartConfig(multipartConfig.toServletConfig());
            logger.info("DispatcherServlet registered with custom MultipartConfig.");
        } else {
            logger.warn("Multipart support DISABLED: No MultipartConfig light found in context.");
        }
        logger.info("DispatcherServlet registered at /*");
    }

    public void registerFilters(ServletContext servletContext) {
        logger.info("Registering web filters...");

        List<LumenFilter> filters = context.getLightContainer()
                .internals()
                .getLightsByType(LumenFilter.class);

        for (LumenFilter filter : filters) {
            String filterName = filter.getClass().getSimpleName();
            FilterRegistration.Dynamic registration = servletContext.addFilter(filterName, filter);
            if (registration != null) {
                registration.addMappingForUrlPatterns(
                        EnumSet.allOf(DispatcherType.class), true, "/*");
                logger.info("Filter [{}] registered and mapped to /*", filterName);
            }
        }
    }

    private static Throwable rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause;
    }
}
