package io.lumen.gleam.ast;

import io.lumen.gleam.EvaluationContext;
import io.lumen.gleam.Expression;

/** Evaluates a named variable: {@code #name}. */
public record VariableExpression(String name) implements Expression {
    @Override
    public Object evaluate(EvaluationContext ctx) {
        return ctx.lookupVariable(name);
    }

    @Override public String toString() { return "#" + name; }
}