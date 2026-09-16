package io.lumen.data.diagnostics;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceConnectionFailureAnalyzerTest {

    private final DataSourceConnectionFailureAnalyzer analyzer = new DataSourceConnectionFailureAnalyzer();

    @Test
    void recognizesSQLException() {
        var failure = new SQLException("Connection refused");

        var analysis = analyzer.analyze(failure);

        assertNotNull(analysis);
        assertTrue(analysis.description().contains("Connection refused"));
        assertTrue(analysis.action().contains("lumen.datasource.url"));
    }

    @Test
    void ignoresUnrelatedExceptions() {
        assertNull(analyzer.analyze(new IllegalStateException("unrelated")));
    }
}