package io.lumen.context;

import io.lumen.core.conditional.Condition;
import io.lumen.core.conditional.ConditionalContext;

class ConditionalOnClassCondition implements Condition {

    private final Class<?>[] classes;
    private final String[] classNames;

    public ConditionalOnClassCondition(Class<?>[] classes, String[] classNames) {
        this.classes = classes;
        this.classNames = classNames;
    }

    @Override
    public boolean matches(ConditionalContext ctx) {
        for (Class<?> clazz : classes) {
            if (!isPresent(clazz.getName())) return false;
        }

        for (String name : classNames) {
            if (!isPresent(name)) return false;
        }

        return true;
    }

    private boolean isPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

