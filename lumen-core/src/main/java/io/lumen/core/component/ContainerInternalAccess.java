package io.lumen.core.component;

public interface ContainerInternalAccess {

    <T> T getLightByType(Class<T> type);

    Object getOrInstantiate(LightInstance light);

    LightInstance getLightInstance(String name);
}
