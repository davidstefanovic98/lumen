package io.lumen.core.logging;

import java.util.logging.LogManager;

public class LoggingConfigurator {
    public static synchronized void configure() {
        LogManager.getLogManager().reset();

        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        cleanupTomcatLogging();
        java.util.logging.ConsoleHandler consoleHandler = new java.util.logging.ConsoleHandler() {
            {
                setFormatter(new LumenLogFormatter());
                setOutputStream(System.out);
                setLevel(java.util.logging.Level.ALL);
            }
        };

        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(java.util.logging.Level.INFO);

        java.util.logging.Logger.getLogger("org.apache").setLevel(java.util.logging.Level.WARNING);
        java.util.logging.Logger.getLogger("io.lumen.core.logging").setLevel(java.util.logging.Level.WARNING);
    }

    public static void cleanupTomcatLogging() {
        java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
        for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
            if (!(handler.getFormatter() instanceof LumenLogFormatter)) {
                rootLogger.removeHandler(handler);
            }
        }
    }
}
