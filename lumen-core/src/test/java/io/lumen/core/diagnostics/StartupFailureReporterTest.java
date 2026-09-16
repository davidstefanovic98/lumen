package io.lumen.core.diagnostics;

import io.lumen.core.exception.AmbiguousLightException;
import io.lumen.core.exception.CircularDependencyException;
import io.lumen.core.exception.NoLightFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class StartupFailureReporterTest {

    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void captureStderr() {
        captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured));
    }

    @AfterEach
    void restoreStderr() {
        System.setErr(originalErr);
    }

    @Test
    void recognizedFailure_printsBannerAndReturnsTrue() {
        boolean reported = StartupFailureReporter.report(new PortInUseException(8080, new RuntimeException("bind failed")));

        assertTrue(reported);
        String output = captured.toString();
        assertTrue(output.contains("APPLICATION FAILED TO START"));
        assertTrue(output.contains("Port 8080 was already in use"));
        assertTrue(output.contains("Action:"));
    }

    @Test
    void unrecognizedFailure_printsNothingAndReturnsFalse() {
        boolean reported = StartupFailureReporter.report(new IllegalStateException("some internal framework bug"));

        assertFalse(reported);
        assertEquals("", captured.toString());
    }

    @Test
    void recognizedFailureNestedInCauseChain_isStillFound() {
        Throwable wrapped = new RuntimeException("outer",
                new RuntimeException("middle", new PortInUseException(9090, null)));

        boolean reported = StartupFailureReporter.report(wrapped);

        assertTrue(reported);
        assertTrue(captured.toString().contains("Port 9090 was already in use"));
    }

    @Test
    void portInUseFailureAnalyzer_ignoresUnrelatedExceptions() {
        var analyzer = new PortInUseFailureAnalyzer();

        assertNull(analyzer.analyze(new IllegalStateException("unrelated")));
        assertNotNull(analyzer.analyze(new PortInUseException(80, null)));
    }

    // -------------------------------------------------------------------------
    // Confirms every built-in analyzer is actually discovered via ServiceLoader
    // (i.e. registered correctly in META-INF/services), not just correct in isolation.
    // -------------------------------------------------------------------------

    @Test
    void circularDependencyException_isDiscoveredAndReported() {
        boolean reported = StartupFailureReporter.report(
                new CircularDependencyException("Circular dependency detected: a -> b -> a"));

        assertTrue(reported);
        assertTrue(captured.toString().contains("Circular dependency detected"));
    }

    @Test
    void noLightFoundException_isDiscoveredAndReported() {
        boolean reported = StartupFailureReporter.report(
                new NoLightFoundException("No light found for type: com.example.Foo"));

        assertTrue(reported);
        assertTrue(captured.toString().contains("No light found for type"));
    }

    @Test
    void ambiguousLightException_isDiscoveredAndReported() {
        boolean reported = StartupFailureReporter.report(
                new AmbiguousLightException("Multiple matches found for Foo: [a, b]"));

        assertTrue(reported);
        assertTrue(captured.toString().contains("@Primary"));
    }
}