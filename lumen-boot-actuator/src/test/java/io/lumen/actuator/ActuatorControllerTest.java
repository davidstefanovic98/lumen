package io.lumen.actuator;

import io.lumen.actuator.health.Health;
import io.lumen.actuator.health.HealthIndicator;
import io.lumen.actuator.health.SimpleHealthIndicator;
import io.lumen.core.context.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActuatorControllerTest {

    private Environment env;

    @BeforeEach
    void setUp() {
        env = new Environment();
        env.setProperty("app.name", "test-app");
        env.setProperty("app.secret", "super-secret");
        env.setProperty("app.port", "8080");
    }

    @Test
    void health_singleIndicatorUp_returnsUp() {
        SimpleHealthIndicator indicator = new SimpleHealthIndicator();
        Health health = indicator.health();
        assertEquals("UP", health.status());
        assertEquals("application", indicator.name());
    }

    @Test
    void health_downIndicator_returnsDown() {
        HealthIndicator down = new HealthIndicator() {
            @Override public Health health() { return Health.down("db unreachable"); }
            @Override public String name()   { return "database"; }
        };
        Health h = down.health();
        assertEquals("DOWN", h.status());
        assertEquals("db unreachable", h.details().get("reason"));
    }

    @Test
    void env_masksSensitiveKeys() {
        ActuatorController controller = new ActuatorController(env);

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) controller.env().get("properties");

        assertEquals("***",      properties.get("app.secret"));
        assertEquals("8080",     properties.get("app.port"));
        assertEquals("test-app", properties.get("app.name"));
    }

    @Test
    void info_containsJavaAndLumenVersions() {
        ActuatorController controller = new ActuatorController(env);

        Map<String, Object> info = controller.info();

        assertTrue(info.containsKey("java"));
        assertTrue(info.containsKey("lumen"));
        @SuppressWarnings("unchecked")
        Map<String, Object> app = (Map<String, Object>) info.get("application");
        assertEquals("test-app", app.get("name"));
    }

    @Test
    void health_degraded_statusIsCorrect() {
        Health h = Health.degraded("high latency");
        assertEquals("DEGRADED", h.status());
        assertEquals("high latency", h.details().get("reason"));
    }
}