package io.lumen.validation;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.util.Suppliers;
import io.lumen.validation.web.ValidRequestBodyValidator;

@Order(2)
public class LumenValidationModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        container.registerExternalInstance(Validator.class, new DefaultValidator());

        // Validate @Valid @RequestBody arguments. Registered only when lumen-web is on the
        // classpath; lumen-web itself has no knowledge of lumen-validation.
        try {
            Class.forName("io.lumen.web.argument.RequestBodyValidator");
            container.registerExternalInstance(ValidRequestBodyValidator.class, new ValidRequestBodyValidator(
                    Suppliers.memoize(() -> container.getLight(Validator.class))));
        } catch (ClassNotFoundException ignored) {
            // lumen-web not on classpath — nothing to bridge
        }
    }
}
