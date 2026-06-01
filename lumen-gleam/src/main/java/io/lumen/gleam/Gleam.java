package io.lumen.gleam;

import io.lumen.gleam.parser.GleamParser;

/**
 * Main entry point for the Gleam expression language.
 *
 * <pre>{@code
 * // Parse once, evaluate many times
 * Expression expr = Gleam.parse("hasRole('ADMIN') && #userId != null");
 * boolean result = (boolean) expr.evaluate(ctx);
 *
 * // Or use the shared caching parser (recommended for production)
 * boolean result2 = (boolean) Gleam.evaluate("hasRole('ADMIN')", ctx);
 * }</pre>
 */
public final class Gleam {

    private static final ExpressionParser SHARED_PARSER =
            new CachingExpressionParser(new GleamParser());

    private Gleam() {}

    /** Parses an expression using the shared caching parser. */
    public static Expression parse(String expression) {
        return SHARED_PARSER.parse(expression);
    }

    /** Parses and immediately evaluates an expression against the given context. */
    public static Object evaluate(String expression, EvaluationContext context) {
        return parse(expression).evaluate(context);
    }

    /** Creates a new {@link GleamParser} (non-caching). */
    public static ExpressionParser newParser() {
        return new GleamParser();
    }

    /** Creates a new {@link CachingExpressionParser} wrapping a fresh {@link GleamParser}. */
    public static CachingExpressionParser newCachingParser() {
        return new CachingExpressionParser(new GleamParser());
    }
}