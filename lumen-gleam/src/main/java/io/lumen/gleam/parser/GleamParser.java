package io.lumen.gleam.parser;

import io.lumen.gleam.Expression;
import io.lumen.gleam.ExpressionParser;
import io.lumen.gleam.ParseException;
import io.lumen.gleam.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive descent parser for Gleam expressions.
 *
 * <p>Grammar (precedence, low → high):
 * <pre>
 * expression   = or
 * or           = and ('||' and)*
 * and          = equality ('&&' equality)*
 * equality     = comparison (('==' | '!=') comparison)?
 * comparison   = unary (('<' | '>' | '<=' | '>=') unary)?
 * unary        = '!' unary | chain
 * chain        = primary ('.' (IDENT '(' args ')' | IDENT))*
 * primary      = '#' IDENT | IDENT '(' args ')' | IDENT | literal | '(' expression ')'
 * literal      = STRING | INTEGER | LONG | DOUBLE | BOOLEAN | NULL
 * args         = (expression (',' expression)*)?
 * </pre>
 */
public class GleamParser implements ExpressionParser {

    @Override
    public Expression parse(String expression) {
        if (expression == null || expression.isBlank())
            throw new ParseException("Expression must not be blank");
        List<Token> tokens = new Lexer(expression.trim()).tokenize();
        Parser p = new Parser(tokens, expression);
        Expression result = p.parseExpression();
        if (p.current().type() != TokenType.EOF)
            throw new ParseException("Unexpected token after expression: " + p.current()
                    + " in: " + expression);
        return result;
    }

    // ── inner parser ──────────────────────────────────────────────────────────

    private static class Parser {

        private final List<Token> tokens;
        private final String source;
        private int pos;

        Parser(List<Token> tokens, String source) {
            this.tokens = tokens;
            this.source = source;
        }

        Expression parseExpression() { return parseOr(); }

        // or = and ('||' and)*
        private Expression parseOr() {
            Expression left = parseAnd();
            while (is(TokenType.OR)) {
                consume();
                left = new BinaryExpression(left, BinaryExpression.Operator.OR, parseAnd());
            }
            return left;
        }

        // and = equality ('&&' equality)*
        private Expression parseAnd() {
            Expression left = parseEquality();
            while (is(TokenType.AND)) {
                consume();
                left = new BinaryExpression(left, BinaryExpression.Operator.AND, parseEquality());
            }
            return left;
        }

        // equality = comparison (('==' | '!=') comparison)?
        private Expression parseEquality() {
            Expression left = parseComparison();
            if (is(TokenType.EQ))  { consume(); return new BinaryExpression(left, BinaryExpression.Operator.EQ,  parseComparison()); }
            if (is(TokenType.NEQ)) { consume(); return new BinaryExpression(left, BinaryExpression.Operator.NEQ, parseComparison()); }
            return left;
        }

        // comparison = unary (('<' | '>' | '<=' | '>=') unary)?
        private Expression parseComparison() {
            Expression left = parseUnary();
            if (is(TokenType.LT))  { consume(); return new BinaryExpression(left, BinaryExpression.Operator.LT,  parseUnary()); }
            if (is(TokenType.GT))  { consume(); return new BinaryExpression(left, BinaryExpression.Operator.GT,  parseUnary()); }
            if (is(TokenType.LTE)) { consume(); return new BinaryExpression(left, BinaryExpression.Operator.LTE, parseUnary()); }
            if (is(TokenType.GTE)) { consume(); return new BinaryExpression(left, BinaryExpression.Operator.GTE, parseUnary()); }
            return left;
        }

        // unary = '!' unary | chain
        private Expression parseUnary() {
            if (is(TokenType.NOT)) { consume(); return new NegationExpression(parseUnary()); }
            return parseChain();
        }

        // chain = primary ('.' (IDENT '(' args ')' | IDENT))*
        private Expression parseChain() {
            Expression node = parsePrimary();
            while (is(TokenType.DOT)) {
                consume(); // consume '.'
                String name = expectIdentifier();
                if (is(TokenType.LPAREN)) {
                    node = new MethodCallExpression(name, node, parseArgs());
                } else {
                    node = new PropertyAccessExpression(name, node);
                }
            }
            return node;
        }

        // primary = '#' IDENT | IDENT '(' args ')' | IDENT | literal | '(' expression ')'
        private Expression parsePrimary() {
            Token t = current();
            return switch (t.type()) {
                case VARIABLE -> {
                    consume();
                    // Allow chaining on variables: #user.name
                    yield new VariableExpression((String) t.value());
                }
                case IDENTIFIER -> {
                    consume();
                    String name = (String) t.value();
                    if (is(TokenType.LPAREN)) {
                        // function call: name(args)
                        yield new FunctionCallExpression(name, parseArgs());
                    }
                    // bare identifier → property on root object
                    yield new PropertyAccessExpression(name, null);
                }
                case STRING, INTEGER, LONG, DOUBLE, BOOLEAN, NULL -> {
                    consume();
                    yield new LiteralExpression(t.value());
                }
                case LPAREN -> {
                    consume();
                    Expression inner = parseExpression();
                    expect(TokenType.RPAREN, "')'");
                    yield inner;
                }
                default -> throw new ParseException(
                        "Unexpected token " + t + " in: " + source);
            };
        }

        // args = '(' (expression (',' expression)*)? ')'
        private List<Expression> parseArgs() {
            expect(TokenType.LPAREN, "'('");
            List<Expression> args = new ArrayList<>();
            if (!is(TokenType.RPAREN)) {
                args.add(parseExpression());
                while (is(TokenType.COMMA)) { consume(); args.add(parseExpression()); }
            }
            expect(TokenType.RPAREN, "')'");
            return args;
        }

        // ── helpers ───────────────────────────────────────────────────────────

        private Token current() { return tokens.get(pos); }

        private boolean is(TokenType type) { return current().type() == type; }

        private Token consume() { return tokens.get(pos++); }

        private String expectIdentifier() {
            Token t = current();
            if (t.type() != TokenType.IDENTIFIER)
                throw new ParseException("Expected identifier, got " + t + " in: " + source);
            consume();
            return (String) t.value();
        }

        private void expect(TokenType type, String description) {
            if (!is(type)) throw new ParseException(
                    "Expected " + description + " but got " + current() + " in: " + source);
            consume();
        }
    }
}