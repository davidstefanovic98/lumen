package io.lumen.web.context;

import jakarta.servlet.ServletContainerInitializer;
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

    public void stop() {
        try {
            tomcat.stop();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }
}
