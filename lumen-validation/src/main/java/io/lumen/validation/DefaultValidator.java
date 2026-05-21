package io.lumen.validation;

import io.lumen.validation.annotation.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class DefaultValidator implements Validator {

    @Override
    public List<ConstraintViolation> validate(Object object) {
        if (object == null) return List.of();

        List<ConstraintViolation> violations = new ArrayList<>();
        Class<?> type = object.getClass();

        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(object);
                    validateField(field, field.getName(), value, violations);
                } catch (IllegalAccessException ignored) {}
            }
            type = type.getSuperclass();
        }

        return violations;
    }

    private void validateField(Field field, String name, Object value, List<ConstraintViolation> violations) {
        if (field.isAnnotationPresent(NotNull.class) && value == null) {
            violations.add(new ConstraintViolation(name, field.getAnnotation(NotNull.class).message()));
            return;
        }

        if (field.isAnnotationPresent(NotBlank.class)) {
            if (value == null || value.toString().isBlank()) {
                violations.add(new ConstraintViolation(name, field.getAnnotation(NotBlank.class).message()));
            }
        }

        if (field.isAnnotationPresent(NotEmpty.class)) {
            boolean empty = value == null
                    || (value instanceof String s && s.isEmpty())
                    || (value instanceof Collection<?> c && c.isEmpty());
            if (empty) {
                violations.add(new ConstraintViolation(name, field.getAnnotation(NotEmpty.class).message()));
            }
        }

        if (field.isAnnotationPresent(Size.class) && value != null) {
            Size ann = field.getAnnotation(Size.class);
            int len = length(value);
            if (len < ann.min()) {
                String msg = ann.message().isEmpty()
                        ? "size must be between " + ann.min() + " and " + ann.max()
                        : ann.message();
                violations.add(new ConstraintViolation(name, msg));
            } else if (len > ann.max()) {
                String msg = ann.message().isEmpty()
                        ? "size must be between " + ann.min() + " and " + ann.max()
                        : ann.message();
                violations.add(new ConstraintViolation(name, msg));
            }
        }

        if (field.isAnnotationPresent(Email.class) && value != null) {
            String s = value.toString();
            if (!s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                violations.add(new ConstraintViolation(name, field.getAnnotation(Email.class).message()));
            }
        }

        if (field.isAnnotationPresent(Min.class) && value != null) {
            Min ann = field.getAnnotation(Min.class);
            long num = toLong(value);
            if (num < ann.value()) {
                String msg = ann.message().isEmpty()
                        ? "must be greater than or equal to " + ann.value()
                        : ann.message();
                violations.add(new ConstraintViolation(name, msg));
            }
        }

        if (field.isAnnotationPresent(Max.class) && value != null) {
            Max ann = field.getAnnotation(Max.class);
            long num = toLong(value);
            if (num > ann.value()) {
                String msg = ann.message().isEmpty()
                        ? "must be less than or equal to " + ann.value()
                        : ann.message();
                violations.add(new ConstraintViolation(name, msg));
            }
        }

        if (field.isAnnotationPresent(io.lumen.validation.annotation.Pattern.class) && value != null) {
            io.lumen.validation.annotation.Pattern ann =
                    field.getAnnotation(io.lumen.validation.annotation.Pattern.class);
            if (!Pattern.matches(ann.regexp(), value.toString())) {
                violations.add(new ConstraintViolation(name, ann.message()));
            }
        }

        if (field.isAnnotationPresent(Positive.class) && value != null) {
            if (toLong(value) <= 0) {
                violations.add(new ConstraintViolation(name, field.getAnnotation(Positive.class).message()));
            }
        }

        if (field.isAnnotationPresent(PositiveOrZero.class) && value != null) {
            if (toLong(value) < 0) {
                violations.add(new ConstraintViolation(name, field.getAnnotation(PositiveOrZero.class).message()));
            }
        }
    }

    private int length(Object value) {
        if (value instanceof String s) return s.length();
        if (value instanceof Collection<?> c) return c.size();
        return 0;
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return 0; }
    }
}