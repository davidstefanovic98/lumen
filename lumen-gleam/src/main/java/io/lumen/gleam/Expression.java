package io.lumen.gleam;

/**
 * A parsed, reusable expression that can be evaluated against an {@link EvaluationContext}.
 * Obtain instances via {@link ExpressionParser#parse(String)}.
 */
public interface Expression {
    Object evaluate(EvaluationContext context);

    default <T> T evaluate(EvaluationContext context, Class<T> expectedType) {
        Object result = evaluate(context);
        if (result == null) return null;
        if (!expectedType.isInstance(result)) {
            throw new EvaluationException(
                "Expression returned " + result.getClass().getSimpleName() +
                " but expected " + expectedType.getSimpleName());
        }
        return expectedType.cast(result);
    }
}