package io.lumen.core.diagnostics;

/**
 * Built-in {@link StartupFailureAnalyzer} for {@link PortInUseException}. Shipped as a default
 * analyzer, the same way Spring Boot ships {@code PortInUseFailureAnalyzer} out of the box.
 */
public class PortInUseFailureAnalyzer implements StartupFailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!(failure instanceof PortInUseException ex)) {
            return null;
        }
        int port = ex.getPort();
        return new FailureAnalysis(
                "Web server failed to start. Port " + port + " was already in use.",
                "Identify and stop the process that's listening on port " + port +
                        ", or configure this application to listen on another port (server.port)."
        );
    }
}