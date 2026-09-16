package io.lumen.web.diagnostics;

import io.lumen.web.exception.PathVariableNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathVariableNotFoundFailureAnalyzerTest {

    private final PathVariableNotFoundFailureAnalyzer analyzer = new PathVariableNotFoundFailureAnalyzer();

    @Test
    void recognizesPathVariableNotFoundException() {
        var failure = new PathVariableNotFoundException(
                "@PathVariable 'userId' on FooController#getFoo has no matching '{userId}' segment in route pattern [GET /foo/{id}]");

        var analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
        assertTrue(analysis.description().contains("userId"));
        assertNotNull(analysis.action());
    }

    @Test
    void ignoresUnrelatedExceptions() {
        assertNull(analyzer.analyze(new IllegalStateException("unrelated")));
    }
}