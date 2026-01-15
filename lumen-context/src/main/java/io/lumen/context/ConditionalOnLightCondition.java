package io.lumen.context;

import io.lumen.core.component.LightContainer;
import io.lumen.core.conditional.Condition;
import io.lumen.core.conditional.ConditionalContext;

class ConditionalOnLightCondition implements Condition {

    private final Class<?>[] types;
    private final String[] names;

    public ConditionalOnLightCondition(Class<?>[] types, String[] names) {
        this.types = types;
        this.names = names;
    }

    @Override
    public boolean matches(ConditionalContext context) {
        LightContainer container = context.getLightContainer();

        for (Class<?> type : types) {
            if (!container.hasLight(type)) {
                return false;
            }
        }

        for (String name : names) {
            if (!container.hasLight(name)) {
                return false;
            }
        }

        return true;
    }
}
