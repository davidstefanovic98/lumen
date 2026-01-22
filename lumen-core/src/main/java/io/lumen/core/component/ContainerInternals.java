package io.lumen.core.component;


import java.util.List;

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

    @Override
    public <T> List<T> getLightsByType(Class<T> type) {
        return container.doGetLightsByType(type);
    }

    @Override
    public <T> T getLightByName(String name) {
        return container.doGetLightByName(name);
    }
}
