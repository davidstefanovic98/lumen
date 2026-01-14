package io.lumen.core.component.processor;

import io.lumen.core.component.LightInstance;

public interface LightProcessor {

    default void beforeInstantiation(LightInstance light) {}

    default Object afterInstantiation(LightInstance light, Object instance) {
        return instance;
    }
}
