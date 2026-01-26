package io.lumen.context;

import io.lumen.core.component.*;
import io.lumen.core.component.processor.ApplicationContextAwareProcessor;
import io.lumen.core.component.processor.DependencyProvider;
import io.lumen.core.component.processor.PostConstructProcessor;
import io.lumen.core.config.Config;
import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.Environment;

import java.util.List;

public class AnnotationApplicationContext implements ApplicationContext {

    private final LightContainer container;
    private final Environment environment;
    private final Config config;

    public AnnotationApplicationContext(Class<?> configClass) {
        this.environment = new Environment();
        DefaultLightCreator lightCreator = getLightCreator();

        this.container = new LightContainer(this,
                new AnnotationLightAnalyzer(),
                lightCreator);
        this.container.setApplicationContext(this);
        this.config = new Config();
        ModuleInitializer.initializeModules(configClass, container);
        new ConfigProcessor(container, configClass);
        container.initialize();
        registerDefaultProcessors();
    }

    public AnnotationApplicationContext() {
        this.environment = new Environment();
        this.container = new LightContainer(this,
                new AnnotationLightAnalyzer(),
                getLightCreator());
        this.container.setApplicationContext(this);
        this.config = new Config();
        registerDefaultProcessors();
    }

    public void scan(Class<?> configClass) {
        new ConfigProcessor(container, configClass);
        ModuleInitializer.initializeModules(configClass, container);
    }

    private DefaultLightCreator getLightCreator() {
        DependencyProvider dependencyProvider =
                new PropertyDependencyProvider(environment);

        CompositeLightInstantiator compositeInstantiator =
                new CompositeLightInstantiator(List.of(
                        new InstanceLightInstantiator(),
                        new FactoryLightInstantiator(),
                        new ClassLightInstantiator(dependencyProvider)
                ));

        return new DefaultLightCreator(compositeInstantiator);
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

    private void registerDefaultProcessors() {
        getLightContainer().addPostProcessor(new ApplicationContextAwareProcessor(this));
        getLightContainer().addPostProcessor(new PostConstructProcessor());
    }
}
