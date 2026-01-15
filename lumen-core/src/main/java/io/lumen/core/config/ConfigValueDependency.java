package io.lumen.core.config;

import io.lumen.core.component.Dependency;

public class ConfigValueDependency extends Dependency {
    private final String key;
    private final Class<?> type;

    public ConfigValueDependency(String key, Class<?> type) {
        super(type, null);
        this.key = key;
        this.type = type;
    }

    public String getKey() { return key; }
    public Class<?> getType() { return type; }
}
