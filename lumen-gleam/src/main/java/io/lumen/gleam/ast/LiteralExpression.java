package io.lumen.gleam.ast;

import io.lumen.gleam.EvaluationContext;
import io.lumen.gleam.Expression;

public record LiteralExpression(Object value) implements Expression {
    @Override
    public Object evaluate(EvaluationContext ctx) {
        return value;
    }

    @Override public String toString() { return String.valueOf(value); }
}