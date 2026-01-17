package io.lumen.core.component;

import io.lumen.core.exception.LightInstantiationException;

public class FactoryLightInstantiator implements LightInstantiator {

    @Override
    public boolean supports(LightInstance light) {
        return light.getMetadata().getDefinition().getSource() == LightDefinition.LightSource.FACTORY;
    }

    @Override
    public Object instantiate(LightInstance light, LightContainer container) {
        try {
            LightFactory factory =
                    (LightFactory) light.getMetadata()
                            .getDefinition()
                            .getSourceData();

            return factory.create(light.getMetadata().getDefinition());

        } catch (Exception e) {
            throw new LightInstantiationException(
                    "Failed to instantiate via factory: " + light.getName(), e
            );
        }
    }
}
