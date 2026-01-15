package io.lumen.core.component;

import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Core container that manages the lifecycle of lights.
 * Orchestrates registration, resolution, and instantiation.
 */
public class LightContainer {
    private final Map<String, LightInstance> lights;
    private final LightAnalyzer analyzer;
    private final LightResolver resolver;
    private final LightInstantiator instantiator;
    private boolean initialized;
    private ApplicationContext context;


    /**
     * Create a container with default components (no annotation support).
     */
    public LightContainer(ApplicationContext context, LightAnalyzer analyzer, LightInstantiator instantiator) {
        this(analyzer, new LightResolver(), instantiator);
        this.context = context;
    }

    /**
     * Create a container with custom components (extension point for context module).
     */
    public LightContainer(LightAnalyzer analyzer, LightResolver resolver, LightInstantiator instantiator) {
        this.lights = new LinkedHashMap<>();
        this.analyzer = analyzer;
        this.resolver = resolver;
        this.instantiator = instantiator;
        this.initialized = false;
    }

    /**
     * Register a class to be managed as a light.
     * Uses simple class name as the light name.
     */
    public void register(Class<?> type) {
        register(type, type.getSimpleName());
    }

    /**
     * Register a class with a specific name.
     */
    public void register(Class<?> type, String name) {
        checkNotInitialized();

        LightDefinition definition = LightDefinition.fromClass(type, name);
        registerDefinition(definition);
    }

    /**
     * Register a pre-created instance.
     */
    public void registerInstance(String name, Object instance) {
        checkNotInitialized();

        LightDefinition definition = LightDefinition.fromInstance(name, instance);
        registerDefinition(definition);
    }

    /**
     * Register a light created by a factory.
     */
    public void registerFactory(String name, Class<?> type, LightFactory factory) {
        checkNotInitialized();

        LightDefinition definition = LightDefinition.fromFactory(name, type, factory);
        registerDefinition(definition);
    }

    public LightDefinition registerFactory(String name, Method method, LightFactory factory, Class<?> type) {
        checkNotInitialized();
        LightDefinition def = LightDefinition.fromFactory(name, method, factory, type);
        registerDefinition(def);
        return def;
    }

    /**
     * Internal method to register a definition.
     */
    protected void registerDefinition(LightDefinition definition) {
        if (definition.getCondition() != null && !definition.getCondition().get()) {
            return;
        }

        if (lights.containsKey(definition.getName())) {
            throw new IllegalStateException(
                    "Light with name '" + definition.getName() + "' already registered"
            );
        }

        LightMetadata metadata = analyzer.analyze(definition);

        LightInstance light = new LightInstance(metadata);

        lights.put(definition.getName(), light);
    }

    public void initialize() {
        if (initialized) {
            throw new IllegalStateException("Container already initialized");
        }

        for (LightInstance light : lights.values()) {
            resolver.resolve(light, lights);
        }

        for (LightInstance light : lights.values()) {
            if (!light.getMetadata().getDefinition().isLazy() &&
                    light.getMetadata().getDefinition().getScope() == ScopeType.SINGLETON) {
                instantiator.instantiate(light, this);
            }
        }

        initialized = true;
    }

    /**
     * Get a light by type.
     * Throws if no matching light found or multiple lights match.
     */
    public <T> T getLight(Class<T> type) {
        checkInitialized();

        List<LightInstance> matches = new ArrayList<>();

        for (LightInstance light : lights.values()) {
            if (type.isAssignableFrom(light.getType())) matches.add(light);
        }

        if (matches.isEmpty())
            throw new NoSuchElementException("No light found for type: " + type.getName());
        if (matches.size() > 1)
            throw new IllegalStateException(
                "Multiple lights found for type: " + type.getName() + ". Use getLight(String) instead."
        );

        return type.cast(getOrInstantiate(matches.getFirst()));
    }

    /**
     * Get a light by name.
     */
    @SuppressWarnings("unchecked")
    public <T> T getLight(String name) {
        checkInitialized();

        LightInstance light = lights.get(name);
        if (light == null) {
            throw new NoSuchElementException(
                    "No light found with name: " + name
            );
        }

        return (T) getOrInstantiate(light);
    }

    /**
     * Get all lights of a specific type.
     */
    public <T> List<T> getLights(Class<T> type) {
        checkInitialized();

        List<T> result = new ArrayList<>();

        for (LightInstance light : lights.values()) {
            if (type.isAssignableFrom(light.getType())) {
                result.add(type.cast(light.getInstance()));
            }
        }

        return result;
    }

    /**
     * Check if a light with the given name exists.
     */
    public boolean hasLight(String name) {
        return lights.containsKey(name);
    }

    /**
     * Check if a light of the given type exists.
     */
    public boolean hasLight(Class<?> type) {
        for (LightInstance light : lights.values()) {
            if (type.isAssignableFrom(light.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all registered light names.
     */
    public Set<String> getLightNames() {
        return Collections.unmodifiableSet(lights.keySet());
    }

    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "Container not initialized. Call initialize() first."
            );
        }
    }

    protected void checkNotInitialized() {
        if (initialized) {
            throw new IllegalStateException(
                    "Cannot modify container after initialization"
            );
        }
    }

    protected LightInstance getLightInstance(String name) {
        return lights.get(name);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void addPostProcessor(LightProcessor processor) {
        instantiator.addPostProcessor(processor);
    }

    public void addPreProcessor(LightProcessor processor) {
        instantiator.addPreProcessor(processor);
    }

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    public Map<String, LightInstance> getLights() {
        return Collections.unmodifiableMap(lights);
    }

    private Object getOrInstantiate(LightInstance light) {
        LightDefinition def = light.getMetadata().getDefinition();

        if (def.getProfile() != null && context != null &&
                !context.getEnvironment().isProfileActive(def.getProfile())) {
            throw new NoSuchElementException(
                    "Light " + def.getName() + " not active in current profile"
            );
        }

        if (def.getCondition() != null && !def.getCondition().get()) {
            throw new NoSuchElementException(
                    "Light " + def.getName() + " did not meet condition"
            );
        }

        if (def.getScope() == ScopeType.PROTOTYPE) {
            return instantiator.instantiatePrototype(light, this);
        }

        if (light.getState() != LightInstance.LightState.READY) {
            instantiator.instantiate(light, this);
        }

        return light.getInstance();
    }

}