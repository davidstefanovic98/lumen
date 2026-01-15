package io.lumen.core.context;

import java.util.*;

public class Environment {
    private final Map<String, String> properties = new HashMap<>();
    private final Set<String> activeProfiles = new LinkedHashSet<>();

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public <T> T getProperty(String key, Class<T> type) {
        String value = properties.get(key);
        if (value == null) return null;

        if (type == String.class) return type.cast(value);
        if (type == Integer.class || type == int.class) return type.cast(Integer.parseInt(value));
        if (type == Boolean.class || type == boolean.class) return type.cast(Boolean.parseBoolean(value));

        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    public void setActiveProfile(String profile) {
        activeProfiles.clear();
        activeProfiles.add(profile);
    }

    public void setActiveProfiles(String... profiles) {
        activeProfiles.clear();
        Collections.addAll(activeProfiles, profiles);
    }

    public Set<String> getActiveProfiles() {
        return Collections.unmodifiableSet(activeProfiles);
    }

    public boolean isProfileActive(String profile) {
        return activeProfiles.contains(profile);
    }
}
