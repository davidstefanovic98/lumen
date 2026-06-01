package io.lumen.gleam.ast;

import io.lumen.gleam.EvaluationContext;
import io.lumen.gleam.Expression;

import java.util.List;

/**
 * Invokes a method on an object: {@code receiver.method(args...)}.
 * When {@code receiver} is {@code null}, invokes on the context's root object.
 */
public record MethodCallExpression(String method, Expression receiver, List<Expression> args)
        implements Expression {

    @Override
    public Object evaluate(EvaluationContext ctx) {
        Object target = receiver != null ? receiver.evaluate(ctx) : ctx.getRootObject();
        Object[] evaluated = args.stream().map(a -> a.evaluate(ctx)).toArray();
        return PropertyAccessor.invoke(target, method, evaluated);
    }

    @Override public String toString() {
        return (receiver != null ? receiver + "." : "") + method + args;
    }
}