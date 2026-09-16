package io.lumen.web.diagnostics;

import io.lumen.core.diagnostics.FailureAnalysis;
import io.lumen.core.diagnostics.StartupFailureAnalyzer;
import io.lumen.web.exception.AmbiguousMappingException;

/**
 * Recognizes two controller handler methods mapped to the same HTTP method and overlapping path,
 * caught by {@code RouteRegistry.register()} at startup.
 */
public class AmbiguousMappingFailureAnalyzer implements StartupFailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!(failure instanceof AmbiguousMappingException)) {
            return null;
        }
        return new FailureAnalysis(
                failure.getMessage(),
                "Change the path or HTTP method on one of the two handler methods so they no longer overlap."
        );
    }
}