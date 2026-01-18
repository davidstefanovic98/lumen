package io.lumen.web.util;

public final class TypeConverter {

    private TypeConverter() {}

    public static Object convert(String value, Class<?> targetType) {
        if (value == null)
            return null;
        if (targetType == String.class)
            return value;
        if (targetType == int.class || targetType == Integer.class)
            return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class)
            return Long.parseLong(value);
        if (targetType == boolean.class || targetType == Boolean.class)
            return Boolean.parseBoolean(value);
        if (targetType == double.class || targetType == Double.class)
            return Double.parseDouble(value);
        throw new IllegalArgumentException("Unsupported parameter type: " + targetType.getName());
    }
}
