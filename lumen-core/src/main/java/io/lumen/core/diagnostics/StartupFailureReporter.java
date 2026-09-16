package io.lumen.core.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Mirrors Spring Boot's {@code FailureAnalyzers}: walks a startup failure's cause chain looking
 * for a {@link StartupFailureAnalyzer} that recognizes it, and if one does, prints a clean
 * "APPLICATION FAILED TO START" report instead of a raw stack trace.
 *
 * <p>The report is written directly to {@code System.err}, deliberately bypassing the logging
 * framework's pattern layout (timestamp/level/logger columns) so it reads as a distinct,
 * human-facing banner rather than another log line - the same effect Spring Boot's own
 * failure-analysis banner has.
 */
public final class StartupFailureReporter {

    private StartupFailureReporter() {
    }

    /**
     * @return true if a matching analyzer was found and the report was printed; false if the
     *         caller should fall back to logging the raw exception itself
     */
    public static boolean report(Throwable failure) {
        List<StartupFailureAnalyzer> analyzers = new ArrayList<>();
        ServiceLoader.load(StartupFailureAnalyzer.class).forEach(analyzers::add);

        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            for (StartupFailureAnalyzer analyzer : analyzers) {
                FailureAnalysis analysis = analyzer.analyze(cause);
                if (analysis != null) {
                    print(analysis);
                    return true;
                }
            }
        }
        return false;
    }

    private static void print(FailureAnalysis analysis) {
        System.err.println();
        System.err.println("***************************");
        System.err.println("APPLICATION FAILED TO START");
        System.err.println("***************************");
        System.err.println();
        System.err.println("Description:");
        System.err.println(analysis.description());
        if (analysis.action() != null) {
            System.err.println();
            System.err.println("Action:");
            System.err.println(analysis.action());
        }
        System.err.println();
    }
}