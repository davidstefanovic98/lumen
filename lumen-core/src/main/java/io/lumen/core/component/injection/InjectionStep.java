package io.lumen.core.component.injection;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;

/**
 * Represents a step in the injection process for a light instance.
 */
public interface InjectionStep {

    void inject(Object instance, LightInstance light, LightContainer container);
}
