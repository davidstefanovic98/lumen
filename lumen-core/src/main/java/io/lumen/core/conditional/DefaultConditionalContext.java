package io.lumen.core.conditional;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightDefinition;
import io.lumen.core.context.Environment;

public class DefaultConditionalContext implements ConditionalContext {
    private final Environment environment;
    private final LightContainer container;
    private final LightDefinition definition;

    public DefaultConditionalContext(Environment environment, LightContainer container, LightDefinition definition) {
        this.environment = environment;
        this.container = container;
        this.definition = definition;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public LightContainer getLightContainer() {
        return container;
    }

    @Override
    public LightDefinition getLightDefinition() {
        return definition;
    }
}
