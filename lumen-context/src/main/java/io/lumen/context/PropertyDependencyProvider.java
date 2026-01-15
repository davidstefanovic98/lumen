package io.lumen.context;

import io.lumen.core.component.Dependency;
import io.lumen.core.component.processor.DependencyProvider;
import io.lumen.core.context.Environment;

import static io.lumen.context.util.Utils.stripPlaceholder;

public class PropertyDependencyProvider implements DependencyProvider {

    private final Environment environment;

    public PropertyDependencyProvider(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object provide(Dependency dependency) {
        if (dependency.getValueKey() != null) {
            return environment.getProperty(stripPlaceholder(dependency.getValueKey()), dependency.getType());
        }
        throw new UnsupportedOperationException("Cannot provide dependency: " + dependency);
    }

    @Override
    public boolean canProvide(Dependency dependency) {
        return dependency.getValueKey() != null;
    }
}
