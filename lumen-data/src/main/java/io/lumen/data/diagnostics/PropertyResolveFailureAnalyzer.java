package io.lumen.data.diagnostics;

import io.lumen.core.diagnostics.FailureAnalysis;
import io.lumen.core.diagnostics.StartupFailureAnalyzer;
import io.lumen.data.exception.PropertyResolveException;

/**
 * Recognizes a derived-query method whose property path doesn't match any field on the entity -
 * almost always a typo in the method name (e.g. {@code findByUsrname}), caught when the
 * repository proxy is built at startup.
 */
public class PropertyResolveFailureAnalyzer implements StartupFailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!(failure instanceof PropertyResolveException)) {
            return null;
        }
        return new FailureAnalysis(
                failure.getMessage(),
                "Check the derived query method name for a typo, or make sure the referenced " +
                        "property is an actual field on the entity (or on a nested property, for a " +
                        "dotted path)."
        );
    }
}