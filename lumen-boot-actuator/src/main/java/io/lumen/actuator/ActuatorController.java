package io.lumen.actuator;

import io.lumen.actuator.health.Health;
import io.lumen.actuator.health.HealthIndicator;
import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.ApplicationContextAware;
import io.lumen.core.context.Environment;
import io.lumen.web.annotation.GetMapping;
import io.lumen.web.annotation.RestController;

import java.util.*;

@RestController
public class ActuatorController implements ApplicationContextAware {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "token", "credential", "private", "passwd"
    );

    private final Environment environment;
    private ApplicationContext applicationContext;

    public ActuatorController(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.applicationContext = ctx;
    }

    @GetMapping("/actuator")
    public Map<String, Object> index() {
        Map<String, Object> links = new LinkedHashMap<>();
        for (String endpoint : List.of("health", "info", "env", "lights")) {
            String path = basePath() + "/" + endpoint;
            links.put(endpoint, Map.of("href", path));
        }
        return Map.of("_links", links);
    }

    @GetMapping("/actuator/health")
    public Map<String, Object> health() {
        List<HealthIndicator> indicators = applicationContext.getLightContainer().getLights(HealthIndicator.class);

        Map<String, Object> components = new LinkedHashMap<>();
        boolean allUp = true;

        for (HealthIndicator indicator : indicators) {
            Health h = indicator.health();
            components.put(indicator.name(), Map.of(
                    "status", h.status(),
                    "details", h.details()
            ));
            if (!"UP".equals(h.status())) allUp = false;
        }

        String overall = allUp ? "UP" : "DOWN";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", overall);
        if (!components.isEmpty()) result.put("components", components);
        return result;
    }

    @GetMapping("/actuator/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("lumen", Map.of("version", "0.1.0-SNAPSHOT"));

        String appName    = environment.getProperty("lumen.application.name",
                            environment.getProperty("app.name", "lumen-application"));
        String appVersion = environment.getProperty("lumen.application.version", "unknown");
        result.put("application", Map.of("name", appName, "version", appVersion));

        result.put("java", Map.of(
                "version", System.getProperty("java.version"),
                "vendor",  System.getProperty("java.vendor")
        ));

        List<String> profiles = List.copyOf(environment.getActiveProfiles());
        if (!profiles.isEmpty()) result.put("profiles", profiles);

        return result;
    }

    @GetMapping("/actuator/env")
    public Map<String, Object> env() {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, String> masked = new TreeMap<>();
        environment.getProperties().forEach((k, v) ->
                masked.put(k, isSensitive(k) ? "***" : v)
        );

        result.put("activeProfiles", List.copyOf(environment.getActiveProfiles()));
        result.put("properties", masked);
        return result;
    }

    @GetMapping("/actuator/lights")
    public Map<String, Object> lights() {
        Map<String, Object> registered = new LinkedHashMap<>();
        applicationContext.getLightContainer().getLights().forEach((name, instance) -> {
            if (instance.getInstance() != null) {
                registered.put(name, Map.of("type", instance.getInstance().getClass().getName()));
            }
        });
        return Map.of("count", registered.size(), "lights", registered);
    }

    private boolean isSensitive(String key) {
        String lower = key.toLowerCase();
        return SENSITIVE_KEYS.stream().anyMatch(lower::contains);
    }

    private String basePath() {
        return environment.getProperty("lumen.actuator.base-path", "/actuator");
    }
}