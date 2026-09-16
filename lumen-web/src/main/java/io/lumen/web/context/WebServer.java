package io.lumen.web.context;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.http.HttpServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.io.File;
import java.util.concurrent.Executor;

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
            if (timeoutSeconds > 0) {
                // Pause the connector immediately so no new connections are accepted
                // while in-flight requests are allowed to complete.
                tomcat.getConnector().pause();
                awaitRequestsDrained(timeoutSeconds * 1000L);
            }
            tomcat.stop();
            tomcat.destroy();
        } catch (LifecycleException e) {
            throw new RuntimeException("Error during graceful shutdown", e);
        }
    }

    /**
     * Waits for in-flight requests to finish, polling the connector's own thread pool rather
     * than delegating to Tomcat's default graceful-stop mechanism
     * ({@code StandardService.setGracefulStopAwaitMillis()}), which waits for every open
     * socket - including idle keep-alive connections a browser tab is holding open - to close.
     * An idle connection with no request being processed doesn't need to be waited on; only
     * requests genuinely in flight do. This returns as soon as those finish instead of always
     * blocking for the full configured grace period, matching how Spring Boot's own Tomcat
     * graceful shutdown behaves.
     */
    private void awaitRequestsDrained(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (getSubmittedRequestCount() == 0) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private int getSubmittedRequestCount() {
        Executor executor = tomcat.getConnector().getProtocolHandler().getExecutor();
        if (executor instanceof ThreadPoolExecutor tpe) {
            return tpe.getSubmittedCount();
        }
        return 0;
    }

    public void stop() {
        stopGracefully(0);
    }
}
