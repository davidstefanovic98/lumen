package io.lumen.core.diagnostics;

import io.lumen.core.exception.CircularDependencyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircularDependencyFailureAnalyzerTest {

    private final CircularDependencyFailureAnalyzer analyzer = new CircularDependencyFailureAnalyzer();

    @Test
    void recognizesCircularDependencyException() {
        var failure = new CircularDependencyException("Circular dependency detected: a -> b -> a");

        FailureAnalysis analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
        assertEquals("Circular dependency detected: a -> b -> a", analysis.description());
        assertNotNull(analysis.action());
    }

    @Test
    void ignoresUnrelatedExceptions() {
        assertNull(analyzer.analyze(new IllegalStateException("unrelated")));
    }
}