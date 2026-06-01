package io.lumen.gleam.ast;

import io.lumen.gleam.EvaluationContext;
import io.lumen.gleam.Expression;

/** Logical negation: {@code !expr}. */
public record NegationExpression(Expression operand) implements Expression {
    @Override
    public Object evaluate(EvaluationContext ctx) {
        return !BinaryExpression.isTruthy(operand.evaluate(ctx));
    }

    @Override public String toString() { return "!" + operand; }
}