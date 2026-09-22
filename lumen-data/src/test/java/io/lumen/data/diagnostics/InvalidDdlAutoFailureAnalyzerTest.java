package io.lumen.data.diagnostics;

import io.lumen.data.exception.InvalidDdlAutoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidDdlAutoFailureAnalyzerTest {

    private final InvalidDdlAutoFailureAnalyzer analyzer = new InvalidDdlAutoFailureAnalyzer();

    @Test
    void recognizesInvalidDdlAutoException() {
        var failure = new InvalidDdlAutoException("'drop-and-create' is not a valid value for lumen.jpa.ddl-auto");

        var analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
        assertEquals("'drop-and-create' is not a valid value for lumen.jpa.ddl-auto", analysis.description());
        assertTrue(analysis.action().contains("create-drop"));
    }

    @Test
    void ignoresUnrelatedExceptions() {
        assertNull(analyzer.analyze(new IllegalStateException("unrelated")));
    }
}