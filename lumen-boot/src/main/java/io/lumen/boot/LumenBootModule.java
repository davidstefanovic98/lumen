package io.lumen.boot;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;

@Order(Integer.MIN_VALUE)
public class LumenBootModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        Environment env = container.getLight(Environment.class);
        if (env != null) {
            ApplicationPropertiesLoader.load(env);
        }
    }
}
