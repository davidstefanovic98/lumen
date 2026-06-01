package io.lumen.context;

import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.context.Environment;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigurationPropertiesProcessor implements LightProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationPropertiesProcessor.class);
    private static final String ANNOTATION_NAME = "io.lumen.context.annotation.ConfigurationProperties";

    private final Environment environment;

    public ConfigurationPropertiesProcessor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        Class<?> type = realClass(instance.getClass());
        String prefix = findPrefix(type);
        if (prefix == null) return instance;

        if (!prefix.isEmpty() && !prefix.endsWith(".")) {
            prefix = prefix + ".";
        }

        bindFields(instance, type, prefix);
        logger.debug("Bound @ConfigurationProperties(\"{}\") to {}", prefix, type.getSimpleName());
        return instance;
    }

    private String findPrefix(Class<?> type) {
        for (Annotation ann : type.getAnnotations()) {
            if (!ANNOTATION_NAME.equals(ann.annotationType().getName())) continue;
            try {
                String value  = (String) ann.annotationType().getMethod("value").invoke(ann);
                String prefix = (String) ann.annotationType().getMethod("prefix").invoke(ann);
                return !value.isEmpty() ? value : prefix;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void bindFields(Object instance, Class<?> type, String prefix) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;

                String key   = prefix + toKebabCase(field.getName());
                String value = environment.getProperty(key);
                if (value != null) {
                    setField(instance, field, value);
                }
            }
            current = current.getSuperclass();
        }
    }

    private void setField(Object instance, Field field, String raw) {
        field.setAccessible(true);
        try {
            field.set(instance, convert(raw.trim(), field.getType()));
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to bind property key to field '" + field.getName() +
                "' (" + field.getType().getSimpleName() + "): " + e.getMessage(), e);
        }
    }

    private Object convert(String value, Class<?> type) {
        if (type == String.class)                            return value;
        if (type == int.class     || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class    || type == Long.class)    return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == double.class  || type == Double.class)  return Double.parseDouble(value);
        if (type == float.class   || type == Float.class)   return Float.parseFloat(value);
        if (type == short.class   || type == Short.class)   return Short.parseShort(value);
        if (type == List.class)
            return Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList());
        return value;
    }

    private String toKebabCase(String name) {
        return name.replaceAll("([A-Z])", "-$1").toLowerCase();
    }

    private Class<?> realClass(Class<?> type) {
        Class<?> current = type;
        while (current != null && current.getName().contains("$ByteBuddy")) {
            current = current.getSuperclass();
        }
        return current != null ? current : type;
    }
}