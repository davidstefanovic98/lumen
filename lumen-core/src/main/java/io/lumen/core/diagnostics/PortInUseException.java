package io.lumen.core.diagnostics;

/**
 * Thrown when the web server fails to bind because the configured port is already in use.
 * Recognized by {@link PortInUseFailureAnalyzer} to produce a clean startup-failure report.
 */
public class PortInUseException extends RuntimeException {

    private final int port;

    public PortInUseException(int port, Throwable cause) {
        super("Port " + port + " is already in use", cause);
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}