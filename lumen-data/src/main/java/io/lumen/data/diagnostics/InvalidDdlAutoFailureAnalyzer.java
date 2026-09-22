package io.lumen.data.diagnostics;

import io.lumen.core.diagnostics.FailureAnalysis;
import io.lumen.core.diagnostics.StartupFailureAnalyzer;
import io.lumen.data.exception.InvalidDdlAutoException;

/**
 * Recognizes an unrecognized {@code lumen.jpa.ddl-auto} value. Without this check, Hibernate
 * treats an unknown {@code hibernate.hbm2ddl.auto} value as "do nothing" — a typo like
 * {@code drop-and-create} silently no-ops instead of failing, which is far more confusing than a
 * clear startup error naming the valid options.
 */
public class InvalidDdlAutoFailureAnalyzer implements StartupFailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!(failure instanceof InvalidDdlAutoException)) {
            return null;
        }
        return new FailureAnalysis(
                failure.getMessage(),
                "Set lumen.jpa.ddl-auto to one of: none, validate, update, create, create-drop. " +
                        "'create' and 'create-drop' drop and recreate the schema on every startup " +
                        "— avoid them outside local development."
        );
    }
}