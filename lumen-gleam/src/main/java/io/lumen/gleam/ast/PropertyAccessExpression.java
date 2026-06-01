package io.lumen.gleam.ast;

import io.lumen.gleam.EvaluationContext;
import io.lumen.gleam.Expression;

/**
 * Reads a named property from an object.
 * When {@code receiver} is {@code null}, reads from the context's root object.
 * Returns {@code null} if the target is {@code null} (safe navigation).
 */
public record PropertyAccessExpression(String property, Expression receiver) implements Expression {
    @Override
    public Object evaluate(EvaluationContext ctx) {
        Object target = receiver != null ? receiver.evaluate(ctx) : ctx.getRootObject();
        if (target == null) return null;
        return PropertyAccessor.get(target, property);
    }

    @Override public String toString() {
        return receiver != null ? receiver + "." + property : property;
    }
}