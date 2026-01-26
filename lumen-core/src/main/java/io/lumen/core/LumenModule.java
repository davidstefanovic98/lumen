package io.lumen.core;

import io.lumen.core.component.LightContainer;

public interface LumenModule {
    /**
     * Called during container initialization to register infrastructure and dynamic components.
     */
    void init(LightContainer container, String... basePackages);
}
