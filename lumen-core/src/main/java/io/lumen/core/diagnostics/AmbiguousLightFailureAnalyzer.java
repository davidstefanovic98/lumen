package io.lumen.core.diagnostics;

import io.lumen.core.exception.AmbiguousLightException;
import io.lumen.core.exception.MultipleLightFoundException;

/**
 * Recognizes multiple candidate lights for a single required type - either during constructor
 * dependency resolution ({@link AmbiguousLightException}) or a direct
 * {@code container.getLight(Class)} call ({@link MultipleLightFoundException}). Both need the
 * same fix: disambiguate which one to use.
 */
public class AmbiguousLightFailureAnalyzer implements StartupFailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!(failure instanceof AmbiguousLightException) && !(failure instanceof MultipleLightFoundException)) {
            return null;
        }
        return new FailureAnalysis(
                failure.getMessage(),
                "Mark exactly one candidate as @Primary, or inject by name/qualifier instead of by type."
        );
    }
}