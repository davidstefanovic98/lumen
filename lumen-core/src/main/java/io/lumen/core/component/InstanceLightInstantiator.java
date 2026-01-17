package io.lumen.core.component;

public class InstanceLightInstantiator implements LightInstantiator {

    @Override
    public boolean supports(LightInstance light) {
        return light.getMetadata().getDefinition().getSource() == LightDefinition.LightSource.INSTANCE;
    }

    @Override
    public Object instantiate(LightInstance light, LightContainer container) {
        return light.getMetadata().getDefinition().getSourceData();
    }
}
