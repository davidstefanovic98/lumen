package io.lumen.web.context;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.http.HttpServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

class WebServer {
    private final Tomcat tomcat;
    private final Context rootContext;

    public WebServer(int port) {
        this.tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        File docBase = new File(System.getProperty("java.io.tmpdir"));
        this.rootContext = tomcat.addContext("", docBase.getAbsolutePath());
    }

    public void addContextListener(ServletContextListener listener) {
        if (rootContext instanceof org.apache.catalina.core.StandardContext standardContext) {
            standardContext.addApplicationLifecycleListener(listener);
        }
    }

    public void addServlet(String name, HttpServlet servlet, String mapping) {
        Tomcat.addServlet(rootContext, name, servlet);
        rootContext.addServletMappingDecoded(mapping, name);
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

    public void stop() {
        try {
            tomcat.stop();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }
}
