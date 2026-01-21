package io.lumen.core.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Fallback adapter using java.util.logging when SLF4J is not available.
 */
class JulLoggerAdapter implements LoggerAdapter {

    static {
        configureJul();
    }

    private static void configureJul() {
        LogManager.getLogManager().reset();

        ConsoleHandler handler = new ConsoleHandler() {
            @Override
            protected synchronized void setOutputStream(java.io.OutputStream out) throws SecurityException {
                super.setOutputStream(System.out);
            }
        };
        handler.setLevel(Level.ALL);
        handler.setFormatter(new LumenLogFormatter());
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.addHandler(handler);
        rootLogger.setLevel(Level.INFO);

        java.util.logging.Logger.getLogger("org.apache.catalina").setLevel(Level.WARNING);
        java.util.logging.Logger.getLogger("org.apache.coyote").setLevel(Level.WARNING);
        java.util.logging.Logger.getLogger("org.apache.tomcat").setLevel(Level.WARNING);
    }
    @Override
    public Logger createLogger(String name) {
        return new JulLogger(java.util.logging.Logger.getLogger(name));
    }

    private record JulLogger(java.util.logging.Logger delegate) implements Logger {

            @Override
            public void trace(String message) {
                delegate.finest(message);
            }

            @Override
            public void trace(String message, Object... args) {
                if (delegate.isLoggable(Level.FINEST)) {
                    delegate.finest(format(message, args));
                }
            }

            @Override
            public void trace(String message, Throwable throwable) {
                delegate.log(Level.FINEST, message, throwable);
            }

            @Override
            public void debug(String message) {
                delegate.fine(message);
            }

            @Override
            public void debug(String message, Object... args) {
                if (delegate.isLoggable(Level.FINE)) {
                    delegate.fine(format(message, args));
                }
            }

            @Override
            public void debug(String message, Throwable throwable) {
                delegate.log(Level.FINE, message, throwable);
            }

            @Override
            public void info(String message) {
                delegate.info(message);
            }

            @Override
            public void info(String message, Object... args) {
                if (delegate.isLoggable(Level.INFO)) {
                    delegate.info(format(message, args));
                }
            }

            @Override
            public void info(String message, Throwable throwable) {
                delegate.log(Level.INFO, message, throwable);
            }

            @Override
            public void warn(String message) {
                delegate.warning(message);
            }

            @Override
            public void warn(String message, Object... args) {
                if (delegate.isLoggable(Level.WARNING)) {
                    delegate.warning(format(message, args));
                }
            }

            @Override
            public void warn(String message, Throwable throwable) {
                delegate.log(Level.WARNING, message, throwable);
            }

            @Override
            public void error(String message) {
                delegate.severe(message);
            }

            @Override
            public void error(String message, Object... args) {
                if (delegate.isLoggable(Level.SEVERE)) {
                    delegate.severe(format(message, args));
                }
            }

            @Override
            public void error(String message, Throwable throwable) {
                delegate.log(Level.SEVERE, message, throwable);
            }

            @Override
            public boolean isTraceEnabled() {
                return delegate.isLoggable(Level.FINEST);
            }

            @Override
            public boolean isDebugEnabled() {
                return delegate.isLoggable(Level.FINE);
            }

            @Override
            public boolean isInfoEnabled() {
                return delegate.isLoggable(Level.INFO);
            }

            @Override
            public boolean isWarnEnabled() {
                return delegate.isLoggable(Level.WARNING);
            }

            @Override
            public boolean isErrorEnabled() {
                return delegate.isLoggable(Level.SEVERE);
            }

            private String format(String message, Object... args) {
                if (args == null || args.length == 0) {
                    return message;
                }

                StringBuilder sb = new StringBuilder();
                int argIndex = 0;
                int start = 0;
                int placeholderIndex;

                while ((placeholderIndex = message.indexOf("{}", start)) != -1) {
                    sb.append(message, start, placeholderIndex);
                    if (argIndex < args.length) {
                        sb.append(args[argIndex++]);
                    } else {
                        sb.append("{}");
                    }
                    start = placeholderIndex + 2;
                }
                sb.append(message.substring(start));

                return sb.toString();
            }
        }
}