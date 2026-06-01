package io.lumen.gleam.parser;

enum TokenType {
    // Literals
    STRING, INTEGER, LONG, DOUBLE, BOOLEAN, NULL,

    // Identifiers & variables
    IDENTIFIER,
    VARIABLE,       // #name

    // Punctuation
    DOT, COMMA, LPAREN, RPAREN,

    // Comparison operators
    EQ, NEQ, LT, GT, LTE, GTE,

    // Logical operators
    AND, OR, NOT,

    EOF
}