package io.lumen.core.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        try {
            Class.forName("org.slf4j.LoggerFactory");
            return new Slf4jLoggerAdapter();
        } catch (ClassNotFoundException e) {
            return new JulLoggerAdapter();
        }
    }
}
