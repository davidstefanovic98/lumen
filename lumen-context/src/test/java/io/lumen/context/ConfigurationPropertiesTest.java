package io.lumen.context;

import io.lumen.context.annotation.ConfigurationProperties;
import io.lumen.core.annotation.Light;
import io.lumen.core.context.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationPropertiesTest {

    private Environment env;
    private ConfigurationPropertiesProcessor processor;

    @BeforeEach
    void setUp() {
        env = new Environment();
        processor = new ConfigurationPropertiesProcessor(env);
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    @ConfigurationProperties("server")
    static class ServerProperties {
        private String host = "localhost";
        private int port = 8080;
        private boolean sslEnabled = false;
    }

    @ConfigurationProperties(prefix = "pool")
    static class PoolProperties {
        private int maxSize = 10;
        private long idleTimeout = 60000L;
        private double factor = 1.5;
    }

    @ConfigurationProperties("app")
    static class AppProperties {
        private String name;
        private List<String> allowedOrigins;
    }

    static class NotAnnotated {
        private String value = "default";
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void bindsStringAndInt() {
        env.setProperty("server.host", "example.com");
        env.setProperty("server.port", "9090");

        ServerProperties props = bind(new ServerProperties());

        assertEquals("example.com", props.host);
        assertEquals(9090, props.port);
        assertFalse(props.sslEnabled); // not set — default preserved
    }

    @Test
    void bindsBooleanAndLong() {
        env.setProperty("server.ssl-enabled", "true");

        ServerProperties props = bind(new ServerProperties());

        assertTrue(props.sslEnabled);
        assertEquals(8080, props.port); // default preserved
    }

    @Test
    void prefixAttributeWorksLikeValue() {
        env.setProperty("pool.max-size", "20");
        env.setProperty("pool.idle-timeout", "30000");
        env.setProperty("pool.factor", "2.5");

        PoolProperties props = bind(new PoolProperties());

        assertEquals(20, props.maxSize);
        assertEquals(30000L, props.idleTimeout);
        assertEquals(2.5, props.factor);
    }

    @Test
    void camelCaseFieldMapsToKebabCaseKey() {
        env.setProperty("pool.max-size", "5");

        PoolProperties props = bind(new PoolProperties());

        assertEquals(5, props.maxSize);
    }

    @Test
    void missingPropertyPreservesFieldDefault() {
        PoolProperties props = bind(new PoolProperties());

        assertEquals(10, props.maxSize);
        assertEquals(60000L, props.idleTimeout);
    }

    @Test
    void bindsCommaSeparatedList() {
        env.setProperty("app.allowed-origins", "http://localhost:3000, http://localhost:5173");

        AppProperties props = bind(new AppProperties());

        assertEquals(List.of("http://localhost:3000", "http://localhost:5173"), props.allowedOrigins);
    }

    @Test
    void nonAnnotatedBeanIsUntouched() {
        env.setProperty("value", "injected");

        NotAnnotated bean = bind(new NotAnnotated());

        assertEquals("default", bean.value);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> T bind(T instance) {
        return (T) processor.afterInstantiation(null, instance);
    }
}