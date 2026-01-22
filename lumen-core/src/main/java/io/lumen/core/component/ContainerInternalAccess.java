package io.lumen.core.component;

import java.util.List;

public interface ContainerInternalAccess {

    <T> T getLightByType(Class<T> type);

    Object getOrInstantiate(LightInstance light);

    LightInstance getLightInstance(String name);

    <T> List<T> getLightsByType(Class<T> type);

    <T> T getLightByName(String name);
}
