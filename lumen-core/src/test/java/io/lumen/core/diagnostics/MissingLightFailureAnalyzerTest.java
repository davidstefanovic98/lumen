package io.lumen.core.diagnostics;

import io.lumen.core.exception.MissingDependencyException;
import io.lumen.core.exception.NoLightFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MissingLightFailureAnalyzerTest {

    private final MissingLightFailureAnalyzer analyzer = new MissingLightFailureAnalyzer();

    @Test
    void recognizesNoLightFoundException() {
        var failure = new NoLightFoundException("No light found for type: com.example.Foo");

        FailureAnalysis analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
        assertEquals("No light found for type: com.example.Foo", analysis.description());
    }

    @Test
    void recognizesMissingDependencyException() {
        var failure = new MissingDependencyException("Missing dependency for light 'foo': required dependency 'bar' not found");

        FailureAnalysis analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
        assertNotNull(analysis.action());
    }

    @Test
    void ignoresUnrelatedExceptions() {
        assertNull(analyzer.analyze(new IllegalStateException("unrelated")));
    }
}