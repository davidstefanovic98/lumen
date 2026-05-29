package io.lumen.web.context;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.http.HttpServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

class WebServer {
    private final Tomcat tomcat;
    private final Context rootContext;

    public WebServer(int port) {
        this.tomcat = new Tomcat();

        File baseDir = new File(System.getProperty("java.io.tmpdir"), "lumen-tomcat-" + port);
        tomcat.setBaseDir(baseDir.getAbsolutePath());

        tomcat.setPort(port);
        tomcat.getConnector();

        this.rootContext = tomcat.addContext("", baseDir.getAbsolutePath());
    }

    public void addServlet(String name, HttpServlet servlet, String mapping) {
        Tomcat.addServlet(rootContext, name, servlet);
        rootContext.addServletMappingDecoded(mapping, name);
    }

    public void addSCI(ServletContainerInitializer sci) {
        rootContext.addServletContainerInitializer(sci, null);
    }

    public void start() {
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    public void await() {
        tomcat.getServer().await();
    }

    /**
     * Returns the port the connector is actually bound to.
     * Useful when server.port=0 (random port) — call after start().
     */
    public int getBoundPort() {
        return tomcat.getConnector().getLocalPort();
    }

    public void stopGracefully(int timeoutSeconds) {
        try {
            if (timeoutSeconds > 0 && tomcat.getService() instanceof StandardService svc) {
                svc.setGracefulStopAwaitMillis(timeoutSeconds * 1000L);
            }
            tomcat.stop();
            tomcat.destroy();
        } catch (LifecycleException e) {
            throw new RuntimeException("Error during graceful shutdown", e);
        }
    }

    public void stop() {
        stopGracefully(0);
    }
}
