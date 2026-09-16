package io.lumen.web.util;

import io.lumen.web.exception.TypeConversionException;

public final class TypeConverter {

    private TypeConverter() {}

    public static Object convert(String value, Class<?> targetType) {
        if (value == null)
            return null;
        if (targetType == String.class)
            return value;
        try {
            if (targetType == int.class || targetType == Integer.class)
                return Integer.parseInt(value);
            if (targetType == long.class || targetType == Long.class)
                return Long.parseLong(value);
            if (targetType == boolean.class || targetType == Boolean.class)
                return Boolean.parseBoolean(value);
            if (targetType == double.class || targetType == Double.class)
                return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new TypeConversionException(
                    "Failed to convert value '" + value + "' to type " + targetType.getSimpleName(), e);
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + targetType.getName());
    }
}
