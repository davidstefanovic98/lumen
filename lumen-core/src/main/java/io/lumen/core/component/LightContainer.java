package io.lumen.core.component;

import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.conditional.ConditionalContext;
import io.lumen.core.conditional.ConditionalEvaluator;
import io.lumen.core.conditional.DefaultConditionalContext;
import io.lumen.core.context.ApplicationContext;
import io.lumen.core.exception.MultipleLightFoundException;
import io.lumen.core.exception.NoLightFoundException;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Core container that manages the lifecycle of lights.
 * Orchestrates registration, resolution, and instantiation.
 */
public class LightContainer {
    private final Map<String, LightInstance> lights = new LinkedHashMap<>();
    private final Map<String, LightDefinition> registeredDefinitions = new LinkedHashMap<>();
    private final LightAnalyzer analyzer;
    private final LightResolver resolver;
    private final DefaultLightCreator instantiator;
    private ApplicationContext context;
    private boolean initialized;
    private final ContainerInternals internals = new ContainerInternals(this);

    public LightContainer(ApplicationContext context, LightAnalyzer analyzer, DefaultLightCreator instantiator) {
        this(analyzer, new LightResolver(), instantiator);
        this.context = context;
    }

    public LightContainer(LightAnalyzer analyzer, LightResolver resolver, DefaultLightCreator instantiator) {
        this.analyzer = analyzer;
        this.resolver = resolver;
        this.instantiator = instantiator;
    }

    public ContainerInternalAccess internals() {
        return internals;
    }

    public void register(Class<?> type) {
        register(type, type.getSimpleName());
    }

    public void register(Class<?> type, String name) {
        LightDefinition def = LightDefinition.fromClass(type, name);
        registerDefinition(def);
    }

    public void registerInstance(String name, Object instance) {
        LightDefinition def = LightDefinition.fromInstance(name, instance);
        registerDefinition(def);
    }

    public void registerFactory(String name, Class<?> type, LightFactory factory) {
        LightDefinition def = LightDefinition.fromFactory(name, type, factory);
        registerDefinition(def);
    }

    public LightDefinition registerFactory(String name, Method method, LightFactory factory, Class<?> type, boolean isPrimary) {
        LightDefinition def = LightDefinition.fromFactory(name, method, factory, type, isPrimary);
        registerDefinition(def);
        return def;
    }

    public void registerExternalInstance(Class<?> type, Object instance) {
        LightDefinition def = LightDefinition.fromInstance(type.getSimpleName(), instance);
        def.setOrigin(LightDefinition.DefinitionOrigin.EXTERNAL);
        def.setType(type);
        LightMetadata metadata = analyzer.analyze(def);

        LightInstance lightInstance = new LightInstance(metadata);
        lightInstance.setInstance(instance);
        lightInstance.setState(LightInstance.LightState.READY);

        lights.put(def.getName(), lightInstance);
    }

    protected void registerDefinition(LightDefinition definition) {
        if (registeredDefinitions.containsKey(definition.getName())) {
            return;
        }
        registeredDefinitions.put(definition.getName(), definition);
    }

    /**
     * Initialize or refresh the container.
     * Clears old instances and re-instantiates eager singletons.
     */
    public void initialize() {
        lights.entrySet().removeIf(entry ->
                entry.getValue().getMetadata().getDefinition().getOrigin() != LightDefinition.DefinitionOrigin.EXTERNAL
        );
        for (LightDefinition def : registeredDefinitions.values()) {
            if (lights.containsKey(def.getName())) {
                continue;
            }

            LightMetadata metadata = analyzer.analyze(def);

            if (def.getProfile() != null && !context.getEnvironment().isProfileActive(def.getProfile())) {
                continue;
            }

            ConditionalContext ctx = new DefaultConditionalContext(context.getEnvironment(), this, def);

            if (!new ConditionalEvaluator().shouldInstantiate(metadata, ctx)) {
                continue;
            }

            LightInstance instance = new LightInstance(metadata);
            lights.put(def.getName(), instance);
        }

        // Resolve dependencies
        for (LightInstance light : lights.values()) {
            resolver.resolve(light, lights);
        }

        // Instantiate eager singletons.
        // Snapshot lights first: post-processors may call registerExternalInstance()
        // which adds new entries to lights — iterating a LinkedHashMap while adding
        // to it throws ConcurrentModificationException.
        for (LightInstance light : new ArrayList<>(lights.values())) {
            LightDefinition def = light.getMetadata().getDefinition();
            if (!def.isLazy() && def.getScope() == ScopeType.SINGLETON) {
                instantiator.create(light, this);
            }
        }

        initialized = true;
    }

    public void refresh() {
        initialize();
    }

    public <T> T getLight(Class<T> type) {
        List<LightInstance> matches = new ArrayList<>();
        for (LightInstance light : lights.values()) {
            if (type.isAssignableFrom(light.getType()))
                matches.add(light);
        }

        // External (READY) instances are available before initialization
        boolean allReady = !matches.isEmpty() &&
                matches.stream().allMatch(l -> l.getState() == LightInstance.LightState.READY);
        if (!allReady) checkInitialized();

        if (matches.isEmpty())
            throw new NoLightFoundException("No light found for type: " + type.getName());
        if (matches.size() > 1)
            throw new MultipleLightFoundException(
                    "Multiple lights found for type: " + type.getName() + ". Use getLight(String) instead."
            );

        return type.cast(getOrInstantiate(matches.getFirst()));
    }

    @SuppressWarnings("unchecked")
    public <T> T getLight(String name) {
        checkInitialized();
        LightInstance light = lights.get(name);
        if (light == null) {
            throw new NoSuchElementException("No light found with name: " + name);
        }

        return (T) getOrInstantiate(light);
    }

    public boolean hasLight(String name) {
        return lights.containsKey(name);
    }

    public boolean hasLight(Class<?> type) {
        for (LightInstance light : lights.values()) {
            if (type.isAssignableFrom(light.getType())) return true;
        }
        return false;
    }

    public Set<String> getLightNames() {
        return Collections.unmodifiableSet(lights.keySet());
    }

    public Map<String, LightInstance> getLights() {
        return Collections.unmodifiableMap(lights);
    }

    public <T> List<T> getLights(Class<T> type) {
        return lights.values().stream()
                .filter(li -> li.getInstance() != null && type.isAssignableFrom(li.getInstance().getClass()))
                .map(li -> type.cast(li.getInstance()))
                .toList();
    }

    public void addPostProcessor(LightProcessor processor) {
        instantiator.addPostProcessor(processor);
    }

    public void addPostProcessor(LightProcessor processor, int order) {
        instantiator.addPostProcessor(processor, order);
    }

    public void addPreProcessor(LightProcessor processor) {
        instantiator.addPreProcessor(processor);
    }

    public List<LightProcessor> getPostProcessors() {
        return instantiator.getPostProcessors();
    }

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    public boolean isInitialized() {
        return initialized;
    }

    LightInstance getLightInstance(String name) {
        return lights.get(name);
    }

    Object doGetOrInstantiate(LightInstance light) {
        LightDefinition def = light.getMetadata().getDefinition();

        if (def.getScope() == ScopeType.PROTOTYPE) {
            return instantiator.createPrototype(light, this);
        }

        if (light.getState() == LightInstance.LightState.INSTANTIATING) {
            return light.getInstance();
        }

        if (light.getState() != LightInstance.LightState.READY) {
            instantiator.create(light, this);
        }

        return light.getInstance();
    }

    <T> T doGetLightByType(Class<T> type) {
        List<LightInstance> matches = new ArrayList<>();
        for (LightInstance light : lights.values()) {
            if (type.isAssignableFrom(light.getType())) {
                matches.add(light);
            }
        }

        if (matches.isEmpty()) {
            return null;
        }
        return type.cast(doGetOrInstantiate(matches.getFirst()));
    }

    <T> T doGetLightByName(String name) {
        LightInstance light = lights.get(name);
        return (T) doGetOrInstantiate(light);
    }


    /**
     * Finds all registered lights that implement or extend the specified type.
     */
    <T> List<T> doGetLightsByType(Class<T> type) {
        List<T> results = new ArrayList<>();

        for (LightInstance light : lights.values()) {
            if (type.isAssignableFrom(light.getType())) {
                Object instance = doGetOrInstantiate(light);
                results.add(type.cast(instance));
            }
        }
        results.sort(OrderComparator.INSTANCE);
        return results;
    }

    private Object getOrInstantiate(LightInstance light) {
        LightDefinition def = light.getMetadata().getDefinition();

        // Evaluate profile at access time
        if (def.getProfile() != null && context != null &&
                !context.getEnvironment().isProfileActive(def.getProfile())) {
            throw new NoSuchElementException(
                    "Light " + def.getName() + " not active in current profile"
            );
        }

        if (def.getCondition() != null && !def.getCondition().get()) {
            throw new NoSuchElementException("Light " + def.getName() + " condition not met");
        }

        // Prototype beans are always created anew
        if (def.getScope() == ScopeType.PROTOTYPE) {
            return instantiator.createPrototype(light, this);
        }

        if (light.getState() == LightInstance.LightState.INSTANTIATING) {
            return light.getInstance();
        }

        // Singleton instantiation if not yet ready
        if (light.getState() != LightInstance.LightState.READY) {
            instantiator.create(light, this);
        }

        return light.getInstance();
    }

    private void checkInitialized() {
        if (!initialized)
            throw new IllegalStateException(
                "Container not initialized. Call initialize() first."
            );
    }
}