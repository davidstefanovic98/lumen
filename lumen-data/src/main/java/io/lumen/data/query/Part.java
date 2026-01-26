package io.lumen.data.query;

public class Part {
    private final String property;
    private final Type type;

    public enum Type {
        SIMPLE_PROPERTY(" = ", true),
        NEGATION(" != ", true),
        GREATER_THAN(" > ", true),
        LESS_THAN(" < ", true),
        LIKE(" LIKE ", true),
        TRUE(" = true", false),
        FALSE(" = false", false),
        IS_NULL(" IS NULL", false),
        IS_NOT_NULL(" IS NOT NULL", false);

        private final String operator;
        private final boolean parameterRequired;

        Type(String operator, boolean parameterRequired) {
            this.operator = operator;
            this.parameterRequired = parameterRequired;
        }

        public String getOperator() { return operator; }
        public boolean requiresParameter() { return parameterRequired; }

        public static Type fromKeyword(String keyword) {
            return switch (keyword) {
                case "Not" -> NEGATION;
                case "GreaterThan" -> GREATER_THAN;
                case "LessThan" -> LESS_THAN;
                case "Like" -> LIKE;
                case "True" -> TRUE;
                case "False" -> FALSE;
                case "IsNull" -> IS_NULL;
                case "IsNotNull" -> IS_NOT_NULL;
                default -> SIMPLE_PROPERTY;
            };
        }
    }

    public Part(String partSource) {
        String[] keywords = {"Not", "GreaterThan", "LessThan", "Like", "True", "False", "IsNotNull", "IsNull"};
        String foundKeyword = "";

        for (String k : keywords) {
            if (partSource.endsWith(k)) {
                foundKeyword = k;
                break;
            }
        }

        this.type = Type.fromKeyword(foundKeyword);
        String prop = partSource.substring(0, partSource.length() - foundKeyword.length());
        this.property = Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
    }

    public String getProperty() { return property; }
    public Type getType() { return type; }
}