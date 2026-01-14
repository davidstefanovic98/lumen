package io.lumen.core.component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an actual light instance with its lifecycle state.
 * Tracks the object through registration -> resolution -> instantiation -> initialization.
 */
public class LightInstance {
    private final LightMetadata metadata;
    private Object instance;
    private LightState state;
    private final Map<Dependency, LightInstance> resolvedDependencies;

    public LightInstance(LightMetadata metadata) {
        this.metadata = metadata;
        this.state = LightState.REGISTERED;
        this.resolvedDependencies = new HashMap<>();

        // If this is a pre-created instance, set it and mark as ready
        if (metadata.getDefinition().getSource() == LightDefinition.LightSource.INSTANCE) {
            this.instance = metadata.getDefinition().getSourceData();
        }
    }

    public enum LightState {
        REGISTERED,     // Definition collected, metadata analyzed
        RESOLVING,      // Currently resolving dependencies (for cycle detection)
        RESOLVED,       // Dependencies identified and matched
        INSTANTIATING,  // Currently creating the instance
        INSTANTIATED,   // Constructor called, object created
        INITIALIZING,   // Running post-construct logic
        READY           // Fully initialized and ready to use
    }

    public LightMetadata getMetadata() {
        return metadata;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public LightState getState() {
        return state;
    }

    public void setState(LightState state) {
        this.state = state;
    }

    public void addResolvedDependency(Dependency dep, LightInstance light) {
        resolvedDependencies.put(dep, light);
    }

    public Collection<LightInstance> getResolvedDependencies() {
        return resolvedDependencies.values();
    }

    public Map<Dependency, LightInstance> getResolvedDependencyMap() {
        return resolvedDependencies;
    }

    public Class<?> getType() {
        return metadata.getDefinition().getType();
    }

    public String getName() {
        return metadata.getDefinition().getName();
    }

    @Override
    public String toString() {
        return "LightInstance{" +
                "name='" + getName() + '\'' +
                ", type=" + getType().getSimpleName() +
                ", state=" + state +
                '}';
    }
}