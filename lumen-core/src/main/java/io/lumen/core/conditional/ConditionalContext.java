package io.lumen.core.conditional;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightDefinition;
import io.lumen.core.context.Environment;

public interface ConditionalContext {
    Environment getEnvironment();
    LightContainer getLightContainer();
    LightDefinition getLightDefinition();
}
