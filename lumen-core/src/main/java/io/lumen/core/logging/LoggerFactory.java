package io.lumen.core.logging;

import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;

public final class LoggerFactory {

    private static final Map<String, Logger> loggerCache = new ConcurrentHashMap<>();
    private static final LoggerAdapter adapter = detectAdapter();

    private LoggerFactory() {}

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(String name) {
        return loggerCache.computeIfAbsent(name, adapter::createLogger);
    }

    private static LoggerAdapter detectAdapter() {
        LoggingConfigurator.configure();
        try {
            Class.forName("org.slf4j.LoggerFactory");
            return new Slf4jLoggerAdapter();
        } catch (ClassNotFoundException e) {
            return new JulLoggerAdapter();
        }
    }
}
