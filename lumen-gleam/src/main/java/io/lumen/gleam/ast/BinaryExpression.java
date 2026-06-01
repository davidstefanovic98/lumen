package io.lumen.gleam.ast;

import io.lumen.gleam.EvaluationContext;
import io.lumen.gleam.EvaluationException;
import io.lumen.gleam.Expression;

/**
 * Binary operator: {@code left OP right}.
 * {@code &&} and {@code ||} short-circuit — the right operand is not evaluated
 * if the left already determines the result.
 */
public record BinaryExpression(Expression left, Operator operator, Expression right)
        implements Expression {

    public enum Operator {
        EQ("=="), NEQ("!="), LT("<"), GT(">"), LTE("<="), GTE(">="), AND("&&"), OR("||");

        private final String symbol;
        Operator(String symbol) { this.symbol = symbol; }
        @Override public String toString() { return symbol; }
    }

    @Override
    public Object evaluate(EvaluationContext ctx) {
        return switch (operator) {
            case AND -> isTruthy(left.evaluate(ctx)) && isTruthy(right.evaluate(ctx));
            case OR  -> isTruthy(left.evaluate(ctx)) || isTruthy(right.evaluate(ctx));
            case EQ  -> { Object l = left.evaluate(ctx); Object r = right.evaluate(ctx);
                          yield l == null ? r == null : l.equals(r); }
            case NEQ -> { Object l = left.evaluate(ctx); Object r = right.evaluate(ctx);
                          yield l == null ? r != null  : !l.equals(r); }
            case LT  -> compare(left.evaluate(ctx), right.evaluate(ctx)) < 0;
            case GT  -> compare(left.evaluate(ctx), right.evaluate(ctx)) > 0;
            case LTE -> compare(left.evaluate(ctx), right.evaluate(ctx)) <= 0;
            case GTE -> compare(left.evaluate(ctx), right.evaluate(ctx)) >= 0;
        };
    }

    static boolean isTruthy(Object v) {
        if (v == null)             return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof String s)  return !s.isEmpty();
        if (v instanceof Number n)  return n.doubleValue() != 0;
        return true;
    }

    @SuppressWarnings("unchecked")
    private static int compare(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Comparable ca) {
            try { return ca.compareTo(b); }
            catch (ClassCastException ignored) {}
        }
        throw new EvaluationException("Cannot compare " + a.getClass().getSimpleName()
                + " with " + b.getClass().getSimpleName());
    }

    @Override public String toString() { return "(" + left + " " + operator + " " + right + ")"; }
}