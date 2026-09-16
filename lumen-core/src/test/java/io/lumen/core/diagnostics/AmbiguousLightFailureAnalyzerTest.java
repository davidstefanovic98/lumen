package io.lumen.core.diagnostics;

import io.lumen.core.exception.AmbiguousLightException;
import io.lumen.core.exception.MultipleLightFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmbiguousLightFailureAnalyzerTest {

    private final AmbiguousLightFailureAnalyzer analyzer = new AmbiguousLightFailureAnalyzer();

    @Test
    void recognizesAmbiguousLightException() {
        var failure = new AmbiguousLightException("Multiple matches found for Foo, but no unique @Primary light exists: [a, b]");

        FailureAnalysis analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
        assertTrue(analysis.action().contains("@Primary"));
    }

    @Test
    void recognizesMultipleLightFoundException() {
        var failure = new MultipleLightFoundException("Multiple lights found for type: Foo. Use getLight(String) instead.");

        FailureAnalysis analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
    }

    @Test
    void ignoresUnrelatedExceptions() {
        assertNull(analyzer.analyze(new IllegalStateException("unrelated")));
    }
}