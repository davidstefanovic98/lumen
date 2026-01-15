package io.lumen.core.conditional;

public interface Condition {
    boolean matches(ConditionalContext context);
}
