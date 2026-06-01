package io.lumen.gleam.parser;

import io.lumen.gleam.ParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Gleam expression string into a flat token list.
 * Supported syntax:
 * <ul>
 *   <li>String literals: {@code 'value'} or {@code "value"}</li>
 *   <li>Number literals: {@code 42}, {@code 42L}, {@code 3.14}</li>
 *   <li>Boolean literals: {@code true}, {@code false}</li>
 *   <li>Null literal: {@code null}</li>
 *   <li>Variables: {@code #name}</li>
 *   <li>Identifiers: {@code myProp}, {@code hasRole}</li>
 *   <li>Operators: {@code == != < > <= >= && || !}</li>
 *   <li>Punctuation: {@code . , ( )}</li>
 * </ul>
 */
class Lexer {

    private final String input;
    private int pos;

    Lexer(String input) {
        this.input = input;
    }

    List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) break;

            char c = input.charAt(pos);

            if (c == '#')                            { tokens.add(readVariable());      continue; }
            if (c == '\'' || c == '"')               { tokens.add(readString(c));        continue; }
            if (Character.isDigit(c))                { tokens.add(readNumber());         continue; }
            if (Character.isLetter(c) || c == '_')  { tokens.add(readIdentifier());     continue; }

            tokens.add(readOperatorOrPunct());
        }
        tokens.add(Token.of(TokenType.EOF, ""));
        return tokens;
    }

    // ── readers ───────────────────────────────────────────────────────────────

    private Token readVariable() {
        pos++; // skip '#'
        String name = readIdentRaw();
        if (name.isEmpty()) throw new ParseException("Expected identifier after '#' at pos " + pos);
        return Token.of(TokenType.VARIABLE, name);
    }

    private Token readString(char quote) {
        pos++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && input.charAt(pos) != quote) {
            char c = input.charAt(pos);
            if (c == '\\' && pos + 1 < input.length()) {
                pos++;
                sb.append(switch (input.charAt(pos)) {
                    case 'n' -> '\n'; case 't' -> '\t'; case 'r' -> '\r';
                    default  -> input.charAt(pos);
                });
            } else {
                sb.append(c);
            }
            pos++;
        }
        if (pos >= input.length()) throw new ParseException("Unterminated string literal");
        pos++; // skip closing quote
        return Token.ofString(sb.toString());
    }

    private Token readNumber() {
        int start = pos;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        boolean isDecimal = pos < input.length() && input.charAt(pos) == '.'
                && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1));
        if (isDecimal) {
            pos++; // consume '.'
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
            return Token.ofDouble(Double.parseDouble(input.substring(start, pos)));
        }
        boolean isLong = pos < input.length() && (input.charAt(pos) == 'L' || input.charAt(pos) == 'l');
        if (isLong) {
            pos++;
            return Token.ofLong(Long.parseLong(input.substring(start, pos - 1)));
        }
        String raw = input.substring(start, pos);
        long val = Long.parseLong(raw);
        return val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE
                ? Token.ofInt((int) val)
                : Token.ofLong(val);
    }

    private Token readIdentifier() {
        String name = readIdentRaw();
        return switch (name) {
            case "true"  -> Token.ofBoolean(true);
            case "false" -> Token.ofBoolean(false);
            case "null"  -> Token.ofNull();
            default      -> Token.of(TokenType.IDENTIFIER, name);
        };
    }

    private String readIdentRaw() {
        int start = pos;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (!Character.isLetterOrDigit(c) && c != '_') break;
            pos++;
        }
        return input.substring(start, pos);
    }

    private Token readOperatorOrPunct() {
        char c = input.charAt(pos);
        char next = pos + 1 < input.length() ? input.charAt(pos + 1) : '\0';
        return switch (c) {
            case '.' -> { pos++;    yield Token.of(TokenType.DOT,    "."); }
            case ',' -> { pos++;    yield Token.of(TokenType.COMMA,  ","); }
            case '(' -> { pos++;    yield Token.of(TokenType.LPAREN, "("); }
            case ')' -> { pos++;    yield Token.of(TokenType.RPAREN, ")"); }
            case '!' -> {
                if (next == '=') { pos += 2; yield Token.of(TokenType.NEQ, "!="); }
                else             { pos++;    yield Token.of(TokenType.NOT, "!");  }
            }
            case '=' -> {
                if (next == '=') { pos += 2; yield Token.of(TokenType.EQ, "=="); }
                throw new ParseException("Unexpected '=' at pos " + pos + "; did you mean '=='?");
            }
            case '<' -> {
                if (next == '=') { pos += 2; yield Token.of(TokenType.LTE, "<="); }
                else             { pos++;    yield Token.of(TokenType.LT,  "<");  }
            }
            case '>' -> {
                if (next == '=') { pos += 2; yield Token.of(TokenType.GTE, ">="); }
                else             { pos++;    yield Token.of(TokenType.GT,  ">");  }
            }
            case '&' -> {
                if (next == '&') { pos += 2; yield Token.of(TokenType.AND, "&&"); }
                throw new ParseException("Expected '&&' at pos " + pos);
            }
            case '|' -> {
                if (next == '|') { pos += 2; yield Token.of(TokenType.OR, "||"); }
                throw new ParseException("Expected '||' at pos " + pos);
            }
            default -> throw new ParseException(
                    "Unexpected character '" + c + "' at pos " + pos + " in: " + input);
        };
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
    }
}