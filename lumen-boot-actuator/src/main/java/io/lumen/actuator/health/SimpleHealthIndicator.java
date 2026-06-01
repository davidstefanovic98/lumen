package io.lumen.actuator.health;

public class SimpleHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up();
    }

    @Override
    public String name() {
        return "application";
    }
}