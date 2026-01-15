package io.lumen.core.component;

import java.util.Objects;

/**
 * Represents a dependency that a light needs.
 * Can match by type, name, or qualifiers (for annotation-based extension).
 */
public class Dependency {
    private final Class<?> type;
    private final String name;
    private final boolean required;
    private final boolean collection;
    private final Class<?>[] qualifiers;
    private final DependencyType depType;
    private final String valueKey;

    public Dependency(Class<?> type) {
        this(type, null, true, false, null, DependencyType.LIGHT, null);
    }

    public Dependency(Class<?> type, String name) {
        this(type, name, true, false, null, DependencyType.LIGHT, null);
    }

    public Dependency(Class<?> type, String name, boolean required, boolean collection, Class<?>[] qualifiers, DependencyType depType, String valueKey) {
        this.type = type;
        this.name = name;
        this.required = required;
        this.collection = collection;
        this.qualifiers = qualifiers;
        this.depType = depType != null ? depType : DependencyType.LIGHT;
        this.valueKey = valueKey;
    }

    /**
     * Check if the given metadata satisfies this dependency.
     */
    public boolean matches(LightMetadata metadata) {
        if (depType == DependencyType.LIGHT) {
            if (name != null && !name.isEmpty() && !name.startsWith("arg")) {
                return metadata.getDefinition().getName().equals(name);
            }
            // Otherwise match by type
            return type.isAssignableFrom(metadata.getDefinition().getType());
        }
      return false;
    }

    public Class<?> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isCollection() {
        return collection;
    }

    public Class<?>[] getQualifiers() {
        return qualifiers;
    }

    public boolean hasQualifiers() {
        return qualifiers != null && qualifiers.length > 0;
    }

    public DependencyType getDepType() {
        return depType;
    }

    public String getValueKey() {
        return valueKey;
    }

    public enum DependencyType {
        LIGHT,
        VALUE
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "type=" + type.getSimpleName() +
                (name != null ? ", name='" + name + '\'' : "") +
                ", required=" + required +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dependency that = (Dependency) o;

        if (!type.equals(that.type)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
