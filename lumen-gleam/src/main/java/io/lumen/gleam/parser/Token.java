package io.lumen.gleam.parser;

record Token(TokenType type, String raw, Object value) {

    /** Token whose value equals its raw text (operators, identifiers, variables). */
    static Token of(TokenType type, String raw) {
        return new Token(type, raw, raw);
    }

    static Token ofString(String value) {
        return new Token(TokenType.STRING, "'" + value + "'", value);
    }

    static Token ofInt(int value) {
        return new Token(TokenType.INTEGER, String.valueOf(value), value);
    }

    static Token ofLong(long value) {
        return new Token(TokenType.LONG, value + "L", value);
    }

    static Token ofDouble(double value) {
        return new Token(TokenType.DOUBLE, String.valueOf(value), value);
    }

    static Token ofBoolean(boolean value) {
        return new Token(TokenType.BOOLEAN, String.valueOf(value), value);
    }

    static Token ofNull() {
        return new Token(TokenType.NULL, "null", null);
    }

    @Override public String toString() { return type + "(" + raw + ")"; }
}