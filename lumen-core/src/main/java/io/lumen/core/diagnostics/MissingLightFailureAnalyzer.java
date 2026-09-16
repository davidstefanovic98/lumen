package io.lumen.core.diagnostics;

import io.lumen.core.exception.MissingDependencyException;
import io.lumen.core.exception.NoLightFoundException;

/**
 * Recognizes a required light that couldn't be found - either via a direct
 * {@code container.getLight(Class)} call ({@link NoLightFoundException}) or during constructor
 * dependency resolution ({@link MissingDependencyException}). Both are the same underlying
 * mistake: nothing registered a light of the requested type.
 */
public class MissingLightFailureAnalyzer implements StartupFailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!(failure instanceof NoLightFoundException) && !(failure instanceof MissingDependencyException)) {
            return null;
        }
        return new FailureAnalysis(
                failure.getMessage(),
                "Make sure a light of this type is registered - annotate the class with " +
                        "@Component/@Service (or provide a @Light factory method) and confirm it's in a " +
                        "scanned package, or check for a typo in the dependency's name/type."
        );
    }
}