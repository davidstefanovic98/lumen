package io.lumen.core.component;

import io.lumen.core.component.processor.DependencyProvider;
import io.lumen.core.exception.LightInstantiationException;
import io.lumen.core.util.ConstructorArgResolver;

import java.lang.reflect.Constructor;

public class ClassLightInstantiator implements LightInstantiator {

    private final DependencyProvider dependencyProvider;

    public ClassLightInstantiator(DependencyProvider dependencyProvider) {
        this.dependencyProvider = dependencyProvider;
    }

    @Override
    public boolean supports(LightInstance light) {
        return light.getMetadata().getDefinition().getSource() == LightDefinition.LightSource.CLASS;
    }

    @Override
    public Object instantiate(LightInstance light, LightContainer container) {
        try {
            Constructor<?> ctor = light.getMetadata().getConstructor();

            Object[] args = ConstructorArgResolver.resolve(
                    light, container, dependencyProvider
            );

            ctor.setAccessible(true);
            return ctor.newInstance(args);

        } catch (Exception e) {
            throw new LightInstantiationException(
                    "Failed to instantiate class light: " + light.getName(), e
            );
        }
    }
}
