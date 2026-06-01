package io.lumen.actuator.health;

import java.util.Map;

public record Health(String status, Map<String, Object> details) {

    public static Health up() {
        return new Health("UP", Map.of());
    }

    public static Health up(Map<String, Object> details) {
        return new Health("UP", details);
    }

    public static Health down(String reason) {
        return new Health("DOWN", Map.of("reason", reason));
    }

    public static Health down(Map<String, Object> details) {
        return new Health("DOWN", details);
    }

    public static Health degraded(String reason) {
        return new Health("DEGRADED", Map.of("reason", reason));
    }
}