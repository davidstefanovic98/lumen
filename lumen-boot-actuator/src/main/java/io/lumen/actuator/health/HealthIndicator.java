package io.lumen.actuator.health;

public interface HealthIndicator {
    Health health();

    default String name() {
        return getClass().getSimpleName()
                .replace("HealthIndicator", "")
                .toLowerCase();
    }
}