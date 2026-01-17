package io.lumen.web.context;

import io.lumen.context.AnnotationApplicationContext;
import io.lumen.web.RouteRegistry;
import io.lumen.web.DispatcherServlet;

public class AnnotationWebApplicationContext implements WebApplicationContext {

    private final AnnotationApplicationContext context;
    private final RouteRegistry routeRegistry;
    private final int port;

    public AnnotationWebApplicationContext(Class<?> configClass, int port) {
        this.context = new AnnotationApplicationContext(configClass);
        this.port = port;
        this.routeRegistry = new RouteRegistry();
        ControllerScanner.scanControllers(context.getLightContainer().getLights().values(), routeRegistry);
    }

    @Override
    public void startWebServer() {
        WebServer server = new WebServer(port);
        server.addServlet("dispatcher", new DispatcherServlet(routeRegistry));
        server.start();
    }

    public AnnotationApplicationContext getApplicationContext() {
        return context;
    }

    public RouteRegistry getRouteRegistry() {
        return routeRegistry;
    }
}

