package io.lumen.core.context;

import io.lumen.core.component.DefaultLightAnalyzer;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightFactory;
import io.lumen.core.component.LightInstantiator;
import io.lumen.core.component.processor.ApplicationContextAwareProcessor;
import io.lumen.core.component.processor.DefaultDependencyProvider;
import io.lumen.core.config.Config;

/**
 * Default implementation of {@link ApplicationContext} using a {@link LightContainer}.
 * <p>
 * This class acts as a simple wrapper around the `LightContainer`, exposing
 * the core registration and retrieval functionality in a convenient
 * ApplicationContext interface.
 * <p>
 * It supports:
 * <ul>
 *     <li>Registering classes for automatic constructor injection.</li>
 *     <li>Registering pre-created instances.</li>
 *     <li>Registering factory-based beans.</li>
 *     <li>Initializing the container and resolving all dependencies.</li>
 * </ul>
 * <p>
 * This implementation is <b>standalone</b> and does not rely on annotations.
 * Once lumen-context is added, a separate `AnnotationBasedApplicationContext`
 * can be used to scan and register beans automatically, while still reusing
 * this underlying LightContainer for dependency resolution.
 */
public class DefaultApplicationContext implements ApplicationContext {

    private final LightContainer lightContainer;
    private final Config config;
    private final Environment environment;

    public DefaultApplicationContext() {
        this.lightContainer = new LightContainer(
                this,
                new DefaultLightAnalyzer(),
                new LightInstantiator(new DefaultDependencyProvider()));
        this.config = new Config();
        this.environment = new Environment();
        registerDefaultProcessors();
    }

    public DefaultApplicationContext(LightContainer container) {
        this.lightContainer = container;
        this.config = new Config();
        this.environment = new Environment();
        registerDefaultProcessors();
    }

    @Override
    public <T> void register(Class<T> clazz) {
        lightContainer.register(clazz);
    }

    @Override
    public <T> T getLight(Class<T> clazz) {
        return lightContainer.getLight(clazz);
    }

    @Override
    public <T> T getLight(String name) {
        return lightContainer.getLight(name);
    }

    @Override
    public void initialize() {
        lightContainer.initialize();
    }

    @Override
    public <T> void registerInstance(String name, T instance) {
        lightContainer.registerInstance(name, instance);
    }

    @Override
    public void registerFactory(String name, Class<?> type, LightFactory factory) {
        lightContainer.registerFactory(name, type, factory);
    }

    @Override
    public LightContainer getLightContainer() {
        return lightContainer;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void refresh() {
        lightContainer.refresh();
    }

    private void registerDefaultProcessors() {
        lightContainer.addPostProcessor(new ApplicationContextAwareProcessor(this));
    }
}
