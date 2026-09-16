package io.lumen.core.diagnostics;

import io.lumen.core.exception.CircularDependencyException;

/**
 * Recognizes a circular dependency between lights - one of the most common confusing errors
 * a developer hits, since the raw exception just shows a resolution path with no guidance.
 */
public class CircularDependencyFailureAnalyzer implements StartupFailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!(failure instanceof CircularDependencyException)) {
            return null;
        }
        return new FailureAnalysis(
                failure.getMessage(),
                "Break the cycle by injecting one side lazily (@Lazy), introducing a setter/field " +
                        "injection point instead of a constructor one, or restructuring the dependency " +
                        "graph so it isn't circular."
        );
    }
}