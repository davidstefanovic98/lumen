package io.lumen.core.diagnostics;

/**
 * SPI for recognizing well-known startup failures and turning them into a clean, actionable
 * report instead of a raw stack trace. Discovered via {@code ServiceLoader}, mirroring the
 * {@code LumenModule} pattern.
 *
 * <p>Register via {@code META-INF/services/io.lumen.core.diagnostics.StartupFailureAnalyzer}.
 */
public interface StartupFailureAnalyzer {

    /**
     * @param failure a single exception from the startup failure's cause chain
     * @return an analysis if this exception is recognized, or {@code null} otherwise
     */
    FailureAnalysis analyze(Throwable failure);
}