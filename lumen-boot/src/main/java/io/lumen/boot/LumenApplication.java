package io.lumen.boot;

import io.lumen.core.diagnostics.StartupFailureReporter;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.context.AnnotationWebApplicationContext;

public final class LumenApplication {

    private static final Logger logger = LoggerFactory.getLogger(LumenApplication.class);

    private LumenApplication() {}

    public static void run(Class<?> configClass, String[] args) {
        System.exit(start(configClass));
    }

    /**
     * The single choke point for every startup failure, regardless of which phase threw it -
     * a module failing during {@code new AnnotationWebApplicationContext(configClass)} (e.g.
     * LumenFlywayModule's eager migration, or Hibernate's eager buildSessionFactory() call, both
     * of which run before startWebServer() is ever invoked) is caught here exactly the same way
     * as a Tomcat-phase failure from startWebServer() itself (e.g. the configured port already
     * being in use). Split as its own method, separate from the {@code System.exit()} call in
     * {@link #run}, so this can be exercised by a test without terminating the JVM.
     */
    static int start(Class<?> configClass) {
        try {
            new AnnotationWebApplicationContext(configClass).startWebServer();
            return 0;
        } catch (Exception e) {
            // Report (or, if no analyzer recognizes it, log with its full stack trace) exactly
            // once here, then exit without letting the JVM's default uncaught-exception handler
            // print a second, redundant stack trace.
            StartupFailureReporter.reportOrLog(e, logger, "Application failed to start");
            return 1;
        }
    }
}
