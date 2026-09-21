package io.lumen.boot;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class LumenApplicationTest {

    static class TestConfig {}

    private Logger logbackLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void captureLogs() {
        logbackLogger = (Logger) LoggerFactory.getLogger(LumenApplication.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logbackLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachAppender() {
        logbackLogger.detachAppender(logAppender);
    }

    /**
     * AlwaysFailingTestModule (registered via this module's test-scope
     * META-INF/services/io.lumen.core.LumenModule) makes `new AnnotationWebApplicationContext(...)`
     * itself throw - i.e. before startWebServer() is ever called - the same phase LumenFlywayModule
     * or LumenDataModule would fail in against an unreachable database. This is exactly the case
     * that used to be silently swallowed: LumenApplication.run()'s catch block assumed every
     * exception it could see had already been reported by startWebServer(), which is only true
     * for failures thrown from that method, not from the constructor call before it.
     */
    @Test
    void start_constructorPhaseModuleFailure_returnsNonZeroInsteadOfPropagating() {
        int exitCode = LumenApplication.start(TestConfig.class);

        assertEquals(1, exitCode, "a startup failure must be reported and exit non-zero, not throw out of start()");
    }

    @Test
    void start_constructorPhaseModuleFailure_isLoggedInsteadOfSilentlySwallowed() {
        LumenApplication.start(TestConfig.class);

        boolean loggedTheFailure = logAppender.list.stream().anyMatch(e ->
                e.getLevel() == Level.ERROR
                        && e.getThrowableProxy() != null
                        && e.getThrowableProxy().getMessage() != null
                        && e.getThrowableProxy().getMessage().contains("AlwaysFailingTestModule"));

        assertTrue(loggedTheFailure,
                "a module failure thrown during context construction (before startWebServer()) must still be " +
                        "reported/logged, not silently exit with nothing printed at all");
    }
}