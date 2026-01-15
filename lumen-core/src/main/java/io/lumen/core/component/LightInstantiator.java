package io.lumen.core.component;

import io.lumen.core.component.processor.DependencyProvider;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.exception.LightInstantiationException;
import io.lumen.core.proxy.ProxyFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Instantiates lights, supports pre- and post-processing hooks.
 */
public class LightInstantiator {

    private final List<LightProcessor> preProcessors = new ArrayList<>();
    private final List<LightProcessor> postProcessors = new ArrayList<>();

    private final DependencyProvider dependencyProvider;

    public LightInstantiator(DependencyProvider dependencyProvider) {
        this.dependencyProvider = dependencyProvider;
    }

    public void addPreProcessor(LightProcessor processor) {
        preProcessors.add(processor);
    }

    public void addPostProcessor(LightProcessor processor) {
        postProcessors.add(processor);
    }

    /**
     * Instantiate a light, including dependency resolution and processor hooks.
     */
    public void instantiate(LightInstance light, LightContainer container) {
        if (light.getState() == LightInstance.LightState.READY)
            return;

        light.setState(LightInstance.LightState.INSTANTIATING);

        try {
            for (LightProcessor processor : preProcessors) {
                processor.beforeInstantiation(light);
            }

            Object instance = createInstance(light, container);

            for (LightProcessor processor : postProcessors) {
                instance = processor.afterInstantiation(light, instance);
            }

            light.setInstance(instance);

            light.setState(LightInstance.LightState.INSTANTIATED);

            initialize(light);

            light.setState(LightInstance.LightState.READY);

        } catch (Exception e) {
            throw new LightInstantiationException(
                    "Failed to instantiate light: " + light.getName(), e
            );
        }
    }

    /**
     * Instantiate a prototype light (always new).
     */
    public Object instantiatePrototype(LightInstance template, LightContainer container) {
        try {
            Object instance = createInstance(template, container);
            for (LightProcessor processor : postProcessors)
                instance = processor.afterInstantiation(template, instance);
            return instance;
        } catch (Exception e) {
            throw new LightInstantiationException("Failed to instantiate prototype: " + template.getName(), e);
        }
    }

    /**
     * Determine which method to create instance (CLASS, FACTORY, INSTANCE).
     */
    protected Object createInstance(LightInstance light, LightContainer container) throws Exception {
        LightDefinition.LightSource source = light.getMetadata().getDefinition().getSource();

        return switch (source) {
            case CLASS -> createFromClass(light, container);
            case FACTORY -> createFromFactory(light, container);
            case INSTANCE -> light.getMetadata().getDefinition().getSourceData();
        };
    }

    /**
     * Create instance using constructor.
     */
    protected Object createFromClass(LightInstance light, LightContainer container) throws Exception {
        Constructor<?> constructor = light.getMetadata().getConstructor();

        for (LightInstance dep : light.getResolvedDependencies()) {
            if (dep.getState() != LightInstance.LightState.READY &&
                    dep.getState() != LightInstance.LightState.INSTANTIATING) {
                instantiate(dep, container);
            }
        }

        Object[] args = gatherConstructorArguments(light, container);

        return constructor.newInstance(args);
    }

    /**
     * Create instance using factory bean.
     */
    protected Object createFromFactory(LightInstance light, LightContainer container) {
        LightFactory factory = (LightFactory) light.getMetadata().getDefinition().getSourceData();
        return factory.create(light.getMetadata().getDefinition());
    }

    /**
     * Collect constructor arguments from resolved dependencies.
     */
    protected Object[] gatherConstructorArguments(LightInstance light, LightContainer container) {
        List<Dependency> constructorDeps = light.getMetadata().getConstructorDeps();
        Map<Dependency, LightInstance> resolvedMap = light.getResolvedDependencyMap();

        List<Object> args = new ArrayList<>();

        for (Dependency dep : constructorDeps) {
            Object arg = null;
            if (dep.isCollection()) {
                List<LightInstance> elements = new ArrayList<>();
                for (Map.Entry<Dependency, LightInstance> entry : resolvedMap.entrySet()) {
                    if (entry.getKey().equals(dep)) elements.add(entry.getValue());
                }
                arg = ProxyFactory.createLazyCollection(container, elements);
            } else if (dep.getValueKey() != null && dependencyProvider.canProvide(dep)) {
                    // Non-light dependency, resolve via provider
                arg = dependencyProvider.provide(dep);
            } else {
                LightInstance depLight = resolvedMap.get(dep);
                if (depLight != null) {
                    arg = depLight.getMetadata().getDefinition().isLazy()
                            ? ProxyFactory.createLazy(container, depLight, dep.getType())
                            : depLight.getInstance();
                } else if (dep.isRequired()) {
                    throw new LightInstantiationException(
                            "Required dependency not resolved: " + dep +
                                    " for light: " + light.getName()
                    );
                }
            }

            args.add(arg);
        }

        return args.toArray();
    }


    /**
     * Post-creation initialization hook (for extension, optional).
     */
    protected void initialize(LightInstance light) {}

    private Object convert(Object value, Class<?> type) {
        if (value == null) return null;
        String str = value.toString();
        if (type == String.class) return str;
        if (type == Integer.class || type == int.class) return Integer.parseInt(str);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(str);
        if (type == Long.class || type == long.class) return Long.parseLong(str);
        return value;
    }
}
