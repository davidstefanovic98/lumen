package io.lumen.web.context;

import jakarta.servlet.http.HttpServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

class WebServer {
    private final Tomcat tomcat;
    private final int port;
    private Context rootContext;
    private final Map<String, HttpServlet> servlets = new HashMap<>();

    public WebServer(int port) {
        this.port = port;
        tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();
    }

    public void start() {
        try {
            File docBase = new File(System.getProperty("java.io.tmpdir"));
            if (!docBase.exists())
                docBase.mkdirs();

            if (rootContext == null) {
                rootContext = tomcat.addContext("", docBase.getAbsolutePath());
            }

            for (Map.Entry<String, HttpServlet> entry : servlets.entrySet()) {
                String name = entry.getKey();
                HttpServlet servlet = entry.getValue();
                Tomcat.addServlet(rootContext, name, servlet);
                rootContext.addServletMappingDecoded("/*", name);
            }
            tomcat.start();
            tomcat.getServer().await();

        } catch (LifecycleException e) {
            throw new RuntimeException("Failed to start Tomcat server", e);
        }
    }

    public Tomcat getTomcat() {
        return tomcat;
    }

    public void addServlet(String servletName, HttpServlet servlet) {
        servlets.put(servletName, servlet);
    }

    public void stop() {
        try {
            tomcat.stop();
        } catch (LifecycleException e) {
            throw new RuntimeException("Failed to stop Tomcat server", e);
        }
    }
}
