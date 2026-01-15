package io.lumen.context;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightFactory;
import io.lumen.core.component.LightInstantiator;
import io.lumen.core.config.Config;
import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.Environment;

public class AnnotationApplicationContext implements ApplicationContext {

    private final LightContainer container;
    private final Environment environment;
    private final Config config;

    public AnnotationApplicationContext(Class<?> configClass) {
        this.environment = new Environment();
        this.container = new LightContainer(this,
                new AnnotationLightAnalyzer(),
                new LightInstantiator(new PropertyDependencyProvider(environment)));
        this.container.setApplicationContext(this);
        this.config = new Config();
        new ConfigProcessor(container, configClass);
        container.initialize();
    }

    @Override
    public <T> void register(Class<T> clazz) {
        container.register(clazz);
    }

    @Override
    public <T> void registerInstance(String name, T instance) {
        container.registerInstance(name, instance);
    }

    @Override
    public void registerFactory(String name, Class<?> type, LightFactory factory) {
        container.registerFactory(name, type, factory);
    }

    @Override
    public <T> T getLight(Class<T> clazz) {
        return container.getLight(clazz);
    }

    @Override
    public <T> T getLight(String name) {
        return container.getLight(name);
    }

    @Override
    public void initialize() {
        container.initialize();
    }

    @Override
    public LightContainer getLightContainer() {
        return container;
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
        container.refresh();
    }
}
