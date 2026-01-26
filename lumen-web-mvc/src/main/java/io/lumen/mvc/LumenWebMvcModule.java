package io.lumen.mvc;

import io.lumen.core.LumenModule;
import io.lumen.core.component.LightContainer;

public class LumenWebMvcModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        container.register(WebMvcLumenInitializer.class);
    }
}
