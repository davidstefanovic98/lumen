package io.lumen.web.util;

public final class TypeInspection {

    private TypeInspection() {}

    public static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Character.class;
    }

    public static boolean isBindableType(Class<?> type) {
        return !isSimpleType(type)
                && !type.getName().startsWith("java.")
                && !type.getName().startsWith("jakarta.");
    }

    private Class<?> getWrapperType(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        if (primitiveType == char.class) return Character.class;
        return primitiveType;
    }
}
