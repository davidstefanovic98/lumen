package io.lumen.web.diagnostics;

import io.lumen.web.exception.AmbiguousMappingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmbiguousMappingFailureAnalyzerTest {

    private final AmbiguousMappingFailureAnalyzer analyzer = new AmbiguousMappingFailureAnalyzer();

    @Test
    void recognizesAmbiguousMappingException() {
        var failure = new AmbiguousMappingException(
                "Ambiguous mapping: [GET /foo] in controller A#a conflicts with existing [GET /foo] in controller B#b");

        var analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
        assertTrue(analysis.description().contains("Ambiguous mapping"));
        assertNotNull(analysis.action());
    }

    @Test
    void ignoresUnrelatedExceptions() {
        assertNull(analyzer.analyze(new IllegalStateException("unrelated")));
    }
}