package io.lumen.core.conditional;

import io.lumen.core.component.LightMetadata;

public class ConditionalEvaluator {

    public boolean shouldInstantiate(LightMetadata metadata, ConditionalContext ctx) {
        if (metadata.getConditions() != null) {
            for (Condition c : metadata.getConditions()) {
                if (!c.matches(ctx))
                    return false;
            }
        }

        Class<? extends Condition>[] condClasses = metadata.getConditionalClasses();
        if (condClasses == null)
            return true;

        for (Class<? extends Condition> clazz : condClasses) {
            try {
                Condition cond = clazz.getDeclaredConstructor().newInstance();
                if (!cond.matches(ctx)) {
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate condition: " + clazz, e);
            }
        }

        return true;
    }
}

