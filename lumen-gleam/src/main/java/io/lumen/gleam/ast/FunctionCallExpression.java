package io.lumen.gleam.ast;

import io.lumen.gleam.EvaluationContext;
import io.lumen.gleam.Expression;

import java.util.List;

/**
 * Calls a named function registered in the {@link EvaluationContext}:
 * {@code functionName(args...)}.
 * This is the primary extensibility hook — security expressions like
 * {@code hasRole('ADMIN')} and {@code isAuthenticated()} are registered
 * as context functions by the module that creates the context.
 */
public record FunctionCallExpression(String name, List<Expression> args) implements Expression {
    @Override
    public Object evaluate(EvaluationContext ctx) {
        Object[] evaluated = args.stream().map(a -> a.evaluate(ctx)).toArray();
        return ctx.invokeFunction(name, evaluated);
    }

    @Override public String toString() { return name + args; }
}