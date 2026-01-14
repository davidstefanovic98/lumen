package io.lumen.core.component;

/**
 * Immutable definition of what a light is and how it should be created.
 * This is the blueprint before any analysis or instantiation happens.
 */
public class LightDefinition {
    private final String name;
    private final Class<?> type;
    private final LightSource source;
    private final Object sourceData;

    private LightDefinition(String name, Class<?> type, LightSource source, Object sourceData) {
        this.name = name;
        this.type = type;
        this.source = source;
        this.sourceData = sourceData;
    }

    public static LightDefinition fromClass(Class<?> type) {
        return fromClass(type, type.getSimpleName());
    }

    public static LightDefinition fromClass(Class<?> type, String name) {
        return new LightDefinition(name, type, LightSource.CLASS, null);
    }

    public static LightDefinition fromInstance(String name, Object instance) {
        return new LightDefinition(name, instance.getClass(), LightSource.INSTANCE, instance);
    }

    public static LightDefinition fromFactory(String name, Class<?> type, LightFactory factory) {
        return new LightDefinition(name, type, LightSource.FACTORY, factory);
    }

    public enum LightSource {
        CLASS,
        INSTANCE,
        FACTORY
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public LightSource getSource() {
        return source;
    }

    public Object getSourceData() {
        return sourceData;
    }

    @Override
    public String toString() {
        return "LightDefinition{" +
                "name='" + name + '\'' +
                ", type=" + type.getSimpleName() +
                ", source=" + source +
                '}';
    }
}
