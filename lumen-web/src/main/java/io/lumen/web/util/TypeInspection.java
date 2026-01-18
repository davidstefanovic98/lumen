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
}
