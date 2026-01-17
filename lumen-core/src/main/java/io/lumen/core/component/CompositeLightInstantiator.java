package io.lumen.core.component;

import io.lumen.core.exception.LightInstantiationException;

import java.util.List;

public class CompositeLightInstantiator {

    private final List<LightInstantiator> instantiators;

    public CompositeLightInstantiator(List<LightInstantiator> instantiators) {
        this.instantiators = instantiators;
    }

    public Object instantiate(LightInstance light, LightContainer container) {
        for (LightInstantiator instantiator : instantiators) {
            if (instantiator.supports(light)) {
                return instantiator.instantiate(light, container);
            }
        }
        throw new LightInstantiationException(
                "No LightInstantiator found for " + light.getName()
        );
    }
}
