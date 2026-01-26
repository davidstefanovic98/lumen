package io.lumen.web.context;

import io.lumen.context.AnnotationApplicationContext;
import io.lumen.core.LumenInitializer;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.logging.StartupBanner;
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
import jakarta.servlet.ServletContext;

import java.util.EnumSet;
import java.util.List;

public class AnnotationWebApplicationContext implements WebApplicationContext {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationWebApplicationContext.class);
    private final AnnotationApplicationContext context;
    private RouteRegistry routeRegistry;
    private final int port;
    private WebServer webServer;

    public AnnotationWebApplicationContext(Class<?> configClass, int port) {
        this.port = port;
        this.context = new AnnotationApplicationContext();

        var container = context.getLightContainer();
        this.routeRegistry = container.internals().getLightByType(RouteRegistry.class);
        context.scan(configClass);
    }

    @Override
    public void startWebServer() {
        try {
            StartupBanner.print(logger);
            webServer = new WebServer(port);

            webServer.addSCI(new LumenServletContainerInitializer(this));

            webServer.start();
            logger.info("Lumen application started successfully on port: {}", port);
            webServer.await();
        } catch (Exception e) {
            logger.error("Critical failure during web server startup", e);
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
                        EnumSet.allOf(DispatcherType.class),
                        true,
                        "/*"
                );
                logger.info("Filter [{}] registered and mapped to /*", filterName);
            }
        }
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

