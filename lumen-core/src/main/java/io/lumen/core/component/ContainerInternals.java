package io.lumen.core.component;


final class ContainerInternals implements ContainerInternalAccess {

    private final LightContainer container;

    ContainerInternals(LightContainer container) {
        this.container = container;
    }

    @Override
    public <T> T getLightByType(Class<T> type) {
        return container.doGetLightByType(type);
    }

    @Override
    public Object getOrInstantiate(LightInstance light) {
        return container.doGetOrInstantiate(light);
    }

    @Override
    public LightInstance getLightInstance(String name) {
        return container.getLightInstance(name);
    }
}
