package io.lumen.web.context;

import io.lumen.context.AnnotationApplicationContext;
import io.lumen.core.LumenInitializer;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.logging.StartupBanner;
import io.lumen.web.DispatcherServlet;
import io.lumen.web.RouteInvoker;
import io.lumen.web.RouteRegistry;
import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.argument.MultipartArgumentResolver;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.filter.LumenFilter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.multipart.MultipartConfig;
import io.lumen.web.resource.ResourceProvider;
import io.lumen.web.resource.StaticResourceResultHandler;
import io.lumen.web.view.ViewResolver;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class AnnotationWebApplicationContext implements WebApplicationContext {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationWebApplicationContext.class);
    private final AnnotationApplicationContext context;
    private final RouteRegistry routeRegistry;
    private final ControllerAdviceRegistry controllerAdviceRegistry;
    private final RouteInvoker routeInvoker;
    private final CompositeMethodArgumentResolver argumentResolver;
    private final HttpMessageConverterRegistry converterRegistry;
    private ViewResolver viewResolver;
    private final int port;
    private WebServer webServer;

    public AnnotationWebApplicationContext(Class<?> configClass, int port) {
        this.port = port;
        this.context = new AnnotationApplicationContext();
        this.routeRegistry = new RouteRegistry();
        this.controllerAdviceRegistry = new ControllerAdviceRegistry();
        this.argumentResolver = new CompositeMethodArgumentResolver();
        this.converterRegistry = new HttpMessageConverterRegistry();
        this.routeInvoker = new RouteInvoker(converterRegistry, argumentResolver);

        var container = context.getLightContainer();
        container.registerExternalInstance(CompositeMethodArgumentResolver.class, argumentResolver);
        container.registerExternalInstance(HttpMessageConverterRegistry.class, converterRegistry);
        container.registerExternalInstance(RouteInvoker.class, routeInvoker);
        container.registerExternalInstance(RouteRegistry.class, routeRegistry);

        logger.info("Lumen Web Application Context: Scanning configuration class [{}]", configClass.getSimpleName());
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
        context.getLightContainer().registerExternalInstance(ServletContext.class, servletContext);
        context.initialize();
        if (context.getLightContainer() != null) {
            List<LumenInitializer> initializers = context.getLightContainer().internals().getLightsByType(LumenInitializer.class);

            for (LumenInitializer initializer : initializers) {
                initializer.onStartup();
            }
        }
        this.argumentResolver.addResolver(new MultipartArgumentResolver(context.getLightContainer()));
        this.registerFilters(servletContext);
        this.registerDispatcherServlet(servletContext);
        this.refreshWebComponents();
    }

    private void registerDispatcherServlet(ServletContext servletContext) {
        var container = context.getLightContainer();

        DispatcherServlet dispatcher = new DispatcherServlet(
                routeRegistry,
                controllerAdviceRegistry,
                container.internals().getLightByType(HttpMessageConverterRegistry.class),
                container.internals().getLightByType(RouteInvoker.class),
                container.internals().getLightByType(ResourceProvider.class),
                container.internals().getLightByType(StaticResourceResultHandler.class)
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

    public AnnotationApplicationContext getApplicationContext() {
        return context;
    }

    public RouteRegistry getRouteRegistry() {
        return routeRegistry;
    }

    public ControllerAdviceRegistry getControllerAdviceRegistry() {
        return controllerAdviceRegistry;
    }

    public ViewResolver getViewResolver() {
        return viewResolver;
    }

    /**
     * This is the Bridge Method that refreshes web components like controllers and exception handlers.
     */
    public void refreshWebComponents() {
        Collection<LightInstance> lights = context.getLightContainer().getLights().values();

        ControllerScanner.scanControllers(lights, routeRegistry);
        GlobalExceptionHandlerScanner.scanForControllerAdvice(lights, controllerAdviceRegistry);

        this.viewResolver = context.getLight(ViewResolver.class);

        logger.info("Web components refreshed: {} routes registered", routeRegistry.getRouteCount());
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

