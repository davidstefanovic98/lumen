package io.lumen.data.query;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Part {
    private final String property;
    private final Type type;

    public enum Type {
        SIMPLE_PROPERTY(" = ", 1),
        NEGATION(" != ", 1),
        GREATER_THAN(" > ", 1),
        GREATER_THAN_EQUAL(" >= ", 1),
        LESS_THAN(" < ", 1),
        LESS_THAN_EQUAL(" <= ", 1),
        LIKE(" LIKE ", 1),
        TRUE(" = true", 0),
        FALSE(" = false", 0),
        IS_NULL(" IS NULL", 0),
        IS_NOT_NULL(" IS NOT NULL", 0),
        BETWEEN(" BETWEEN ", 2),
        IN(" IN ", 1),
        CONTAINING(" LIKE ", 1),
        STARTING_WITH(" LIKE ", 1),
        ENDING_WITH(" LIKE ", 1),
        IGNORE_CASE(" = ", 1),
        CONTAINING_IGNORE_CASE(" LIKE ", 1),
        STARTING_WITH_IGNORE_CASE(" LIKE ", 1),
        ENDING_WITH_IGNORE_CASE(" LIKE ", 1);

        private final String operator;
        private final int numberOfParameters;

        Type(String operator, int numberOfParameters) {
            this.operator = operator;
            this.numberOfParameters = numberOfParameters;
        }

        public String getOperator()         { return operator; }
        public int getNumberOfParameters()  { return numberOfParameters; }
        public boolean requiresParameter()  { return numberOfParameters > 0; }

        public static Type fromKeyword(String keyword) {
            return switch (keyword) {
                case "ContainingIgnoreCase"   -> CONTAINING_IGNORE_CASE;
                case "StartingWithIgnoreCase" -> STARTING_WITH_IGNORE_CASE;
                case "EndingWithIgnoreCase"   -> ENDING_WITH_IGNORE_CASE;
                case "GreaterThanEqual"       -> GREATER_THAN_EQUAL;
                case "LessThanEqual"          -> LESS_THAN_EQUAL;
                case "IsNotNull"              -> IS_NOT_NULL;
                case "IsNull"                 -> IS_NULL;
                case "GreaterThan"            -> GREATER_THAN;
                case "LessThan"               -> LESS_THAN;
                case "Between"                -> BETWEEN;
                case "Containing"             -> CONTAINING;
                case "StartingWith"           -> STARTING_WITH;
                case "EndingWith"             -> ENDING_WITH;
                case "IgnoreCase"             -> IGNORE_CASE;
                case "Not"                    -> NEGATION;
                case "Like"                   -> LIKE;
                case "True"                   -> TRUE;
                case "False"                  -> FALSE;
                case "In"                     -> IN;
                default                       -> SIMPLE_PROPERTY;
            };
        }
    }

    // Ordered longest-first so endsWith checks don't mis-match a shorter keyword
    private static final String[] KEYWORDS = {
        "ContainingIgnoreCase", "StartingWithIgnoreCase", "EndingWithIgnoreCase",
        "GreaterThanEqual", "LessThanEqual",
        "IsNotNull", "IsNull",
        "GreaterThan", "LessThan",
        "Between", "Containing", "StartingWith", "EndingWith",
        "IgnoreCase",
        "Not", "Like", "True", "False",
        "In"
    };

    public Part(String partSource) {
        String foundKeyword = "";
        for (String k : KEYWORDS) {
            if (partSource.endsWith(k)) {
                foundKeyword = k;
                break;
            }
        }
        this.type = Type.fromKeyword(foundKeyword);
        String prop = partSource.substring(0, partSource.length() - foundKeyword.length());
        this.property = toJpqlPath(prop);
    }

    private static String toJpqlPath(String prop) {
        if (!prop.contains("_")) {
            return Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
        }
        return Arrays.stream(prop.split("_"))
                .map(p -> Character.toLowerCase(p.charAt(0)) + p.substring(1))
                .collect(Collectors.joining("."));
    }

    public String getProperty() { return property; }
    public Type getType()       { return type; }
}
