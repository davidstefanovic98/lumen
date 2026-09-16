package io.lumen.core.diagnostics;

/**
 * A human-readable diagnosis of a startup failure, produced by a {@link StartupFailureAnalyzer}.
 *
 * @param description what went wrong
 * @param action      how to fix it, or {@code null} if there's nothing actionable to suggest
 */
public record FailureAnalysis(String description, String action) {
}