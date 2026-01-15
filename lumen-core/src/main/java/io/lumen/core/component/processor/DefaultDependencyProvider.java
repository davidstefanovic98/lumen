package io.lumen.core.component.processor;

import io.lumen.core.component.Dependency;
import io.lumen.core.exception.LightInstantiationException;

public class DefaultDependencyProvider implements DependencyProvider {

    @Override
    public Object provide(Dependency dependency) {
        throw new LightInstantiationException(
                "Cannot resolve external dependency: " + dependency +
                        ". No DependencyProvider configured. " +
                        "If you need @Value or other external dependencies, use lumen-context module."
        );
    }

    @Override
    public boolean canProvide(Dependency dependency) {
        return false;
    }
}
