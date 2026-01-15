package io.lumen.context;

import io.lumen.core.conditional.Condition;
import io.lumen.core.conditional.ConditionalContext;

class ConditionalOnPropertyCondition implements Condition {

    private final String name;
    private final String value;
    private final boolean matchIfMissing;

    public ConditionalOnPropertyCondition(String name, String value, boolean matchIfMissing) {
        this.name = name;
        this.value = value;
        this.matchIfMissing = matchIfMissing;
    }

    @Override
    public boolean matches(ConditionalContext ctx) {
        String prop = ctx.getEnvironment().getProperty(name);
        if (prop == null) return matchIfMissing;
        return value.isEmpty() || prop.equals(value);
    }
}
