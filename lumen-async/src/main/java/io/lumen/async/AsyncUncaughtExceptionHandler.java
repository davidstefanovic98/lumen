package io.lumen.async;

import java.lang.reflect.Method;

/**
 * Handles exceptions thrown by void @Async methods (which have no caller to propagate to).
 * Register a bean implementing this interface to override the default logging behaviour.
 */
public interface AsyncUncaughtExceptionHandler {
    void handleUncaughtException(Throwable ex, Method method, Object... args);
}