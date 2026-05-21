package io.lumen.core.logging;

import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.Level;

public final class LoggingConfigurator {

    private static volatile boolean configured = false;

    private LoggingConfigurator() {}

    public static synchronized void configure() {
        if (configured) return;
        configured = true;

        // Remove all existing JUL handlers from the root logger so Tomcat's
        // built-in handlers don't produce duplicate or raw output.
        SLF4JBridgeHandler.removeHandlersForRootLogger();

        // Install the bridge: all JUL records (including Tomcat's) flow to
        // SLF4J and from there to Logback, whose logback.xml controls the format.
        SLF4JBridgeHandler.install();

        // Allow all JUL records to reach the bridge; Logback handles filtering.
        java.util.logging.Logger.getLogger("").setLevel(Level.ALL);
    }
}