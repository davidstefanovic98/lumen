package io.lumen.core.config;

import io.lumen.core.context.Environment;

import java.util.HashMap;
import java.util.Map;

public class Config {
    private final Map<String, String> properties = new HashMap<>();
    private Environment environment;

    public Config(Environment env) {
        this.environment = env;
    }

    public Config() {
        this.environment = new Environment();
    }

    public void set(String key, String value) {
        properties.put(key, value);
    }

    public String get(String key) {
        return properties.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        String value = properties.get(key);
        if (value == null) return null;

        if (type == String.class) return type.cast(value);
        if (type == Integer.class || type == int.class) return type.cast(Integer.parseInt(value));
        if (type == Boolean.class || type == boolean.class) return type.cast(Boolean.parseBoolean(value));

        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    // --- environment-aware ---
    public boolean isProfileActive(String profile) {
        return environment != null && environment.isProfileActive(profile);
    }
}

