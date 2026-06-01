package io.lumen.gleam;

/**
 * Parses an expression string into a reusable {@link Expression}.
 * Implementations should be thread-safe; parsed {@link Expression} objects
 * are immutable and safe to cache and share.
 */
public interface ExpressionParser {
    /**
     * Parses the expression string.
     * @throws ParseException if the expression is syntactically invalid
     */
    Expression parse(String expression);
}