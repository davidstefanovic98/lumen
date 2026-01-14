package io.lumen.core.component;

import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.exception.LightInstantiationException;

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
            instantiate(dep, container);
        }

        Object[] args = gatherConstructorArguments(light);

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
    protected Object[] gatherConstructorArguments(LightInstance light) {
        List<Dependency> constructorDeps = light.getMetadata().getConstructorDeps();
        Map<Dependency, LightInstance> resolvedMap = light.getResolvedDependencyMap();

        List<Object> args = new ArrayList<>();

        for (Dependency dep : constructorDeps) {
            if (dep.isCollection()) {
                List<Object> collection = new ArrayList<>();
                for (Map.Entry<Dependency, LightInstance> entry : resolvedMap.entrySet()) {
                    if (entry.getKey().equals(dep)) {
                        collection.add(entry.getValue().getInstance());
                    }
                }
                args.add(collection);
            } else {
                LightInstance depLight = resolvedMap.get(dep);
                if (depLight != null) {
                    args.add(depLight.getInstance());
                } else if (!dep.isRequired()) {
                    args.add(null);
                } else {
                    throw new LightInstantiationException(
                            "Required dependency not resolved: " + dep +
                                    " for light: " + light.getName()
                    );
                }
            }
        }

        return args.toArray();
    }

    /**
     * Post-creation initialization hook (for extension, optional).
     */
    protected void initialize(LightInstance light) {}
}
