package io.lumen.context.util;

public class Utils {
    public static String stripPlaceholder(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            return key.substring(2, key.length() - 1);
        }
        return key;
    }
}
