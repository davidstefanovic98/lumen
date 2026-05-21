package io.lumen.validation;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;

@Order(2)
public class LumenValidationModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        container.registerExternalInstance(Validator.class, new DefaultValidator());
    }
}