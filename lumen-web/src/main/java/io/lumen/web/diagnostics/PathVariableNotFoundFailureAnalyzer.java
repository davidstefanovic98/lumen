package io.lumen.web.diagnostics;

import io.lumen.core.diagnostics.FailureAnalysis;
import io.lumen.core.diagnostics.StartupFailureAnalyzer;
import io.lumen.web.exception.PathVariableNotFoundException;

/**
 * Recognizes a {@code @PathVariable} name with no matching {@code {name}} segment in its own
 * route's pattern, caught by {@code RouteRegistry.register()} at startup.
 */
public class PathVariableNotFoundFailureAnalyzer implements StartupFailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!(failure instanceof PathVariableNotFoundException)) {
            return null;
        }
        return new FailureAnalysis(
                failure.getMessage(),
                "Check for a typo in the @PathVariable name or in the route pattern's '{...}' segment."
        );
    }
}