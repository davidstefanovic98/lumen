package io.lumen.data.diagnostics;

import io.lumen.data.exception.PropertyResolveException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PropertyResolveFailureAnalyzerTest {

    private final PropertyResolveFailureAnalyzer analyzer = new PropertyResolveFailureAnalyzer();

    @Test
    void recognizesPropertyResolveException() {
        var failure = new PropertyResolveException("Entity User has no property path 'usrname'");

        var analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
        assertEquals("Entity User has no property path 'usrname'", analysis.description());
    }

    @Test
    void ignoresUnrelatedExceptions() {
        assertNull(analyzer.analyze(new IllegalStateException("unrelated")));
    }
}