package io.lumen.core.component;

import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.exception.LightInstantiationException;
import io.lumen.core.component.injection.FieldInjectionStep;
import io.lumen.core.component.injection.InjectionStep;
import io.lumen.core.component.injection.SetterInjectionStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Instantiates lights, supports pre- and post-processing hooks.
 */
public class DefaultLightCreator {

    private record OrderedEntry(int order, int index, LightProcessor processor) {}

    private static final Comparator<OrderedEntry> ENTRY_ORDER =
            Comparator.comparingInt(OrderedEntry::order).thenComparingInt(OrderedEntry::index);

    private final CompositeLightInstantiator instantiator;
    private final List<InjectionStep> injectionSteps;
    private final List<LightProcessor> preProcessors;
    private final List<OrderedEntry> postProcessors;
    private int nextPostProcessorIndex = 0;

    public DefaultLightCreator(
            CompositeLightInstantiator instantiator
    ) {
        this.instantiator = instantiator;

        this.injectionSteps = List.of(
                new FieldInjectionStep(),
                new SetterInjectionStep()
        );

        this.preProcessors = new ArrayList<>();
        this.postProcessors = new ArrayList<>();
    }

    public void addPreProcessor(LightProcessor processor) {
        preProcessors.add(processor);
    }

    public void addPostProcessor(LightProcessor processor) {
        addPostProcessor(processor, Integer.MAX_VALUE);
    }

    public void addPostProcessor(LightProcessor processor, int order) {
        postProcessors.add(new OrderedEntry(order, nextPostProcessorIndex++, processor));
        postProcessors.sort(ENTRY_ORDER);
    }

    public List<LightProcessor> getPreProcessors() {
        return preProcessors;
    }

    public List<LightProcessor> getPostProcessors() {
        return postProcessors.stream().map(OrderedEntry::processor).toList();
    }

    public void create(LightInstance light, LightContainer container) {
        if (light.getState() == LightInstance.LightState.READY)
           return;

        light.setState(LightInstance.LightState.INSTANTIATING);

        try {
            for (LightProcessor p : preProcessors)
                p.beforeInstantiation(light);

            Object instance = instantiator.instantiate(light, container);

            for (InjectionStep step : injectionSteps)
                step.inject(instance, light, container);

            for (OrderedEntry entry : postProcessors)
                instance = entry.processor().afterInstantiation(light, instance);

            light.setInstance(instance);
            light.setState(LightInstance.LightState.READY);
        } catch (Exception e) {
            throw new LightInstantiationException("Failed to create light: " + light.getName(), e);
        }
    }


    public Object createPrototype(LightInstance light, LightContainer container) {
        light.setState(LightInstance.LightState.INSTANTIATING);

        Object instance = instantiator.instantiate(light, container);

        for (InjectionStep step : injectionSteps)
            step.inject(instance, light, container);

        for (OrderedEntry entry : postProcessors)
            instance = entry.processor().afterInstantiation(light, instance);

        return instance;
    }
}
