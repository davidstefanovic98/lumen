package io.lumen.core.component;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Immutable definition of what a light is and how it should be created.
 * This is the blueprint before any analysis or instantiation happens.
 */
public class LightDefinition {
    private final String name;
    private final Class<?> type;
    private final LightSource source;
    private final Object sourceData;
    private boolean lazy;
    private ScopeType scope = ScopeType.SINGLETON;
    private String profile;
    private Supplier<Boolean> condition;
    private Executable executable;

    private LightDefinition(String name, Class<?> type, LightSource source, Object sourceData) {
        this.name = name;
        this.type = type;
        this.source = source;
        this.sourceData = sourceData;
        this.executable = null;
    }

    private LightDefinition(String name, Class<?> type, LightSource source, Executable executable) {
        this.name = name;
        this.type = type;
        this.source = source;
        this.executable = executable;
        this.sourceData = null;
    }

    private LightDefinition(String name, Method method, LightSource source, Object sourceData, Class<?> type) {
        this.name = name;
        this.executable = method;
        this.source = source;
        this.sourceData = sourceData;
        this.type = type;
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

    public static LightDefinition fromFactory(
            String name,
            Method method,
            LightFactory factory,
            Class<?> type
    ) {
        return new LightDefinition(name, method, LightSource.FACTORY, factory, type);
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

    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public ScopeType getScope() {
        return scope;
    }

    public void setScope(ScopeType scope) {
        this.scope = scope;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public Supplier<Boolean> getCondition() {
        return condition;
    }

    public void setCondition(Supplier<Boolean> condition) {
        this.condition = condition;
    }

    public Executable getExecutable() {
        return executable;
    }

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    @Override
    public String toString() {
        return "LightDefinition{" +
                "name='" + name + '\'' +
                ", type=" + type.getSimpleName() +
                ", source=" + source +
                ", lazy=" + lazy +
                ", scope=" + scope +
                ", profile='" + profile + '\'' +
                '}';
    }
}
