package io.lumen.gleam.ast;

import io.lumen.gleam.EvaluationException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflective property / method access used by AST nodes.
 * Resolution order for a property named {@code foo}:
 * <ol>
 *   <li>{@code getFoo()}</li>
 *   <li>{@code isFoo()} (boolean getter)</li>
 *   <li>{@code foo()} (record component accessor or plain no-arg method)</li>
 *   <li>public field {@code foo}</li>
 * </ol>
 */
final class PropertyAccessor {

    private PropertyAccessor() {}

    static Object get(Object target, String name) {
        if (target == null) return null;
        Class<?> type = target.getClass();

        String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);

        for (String candidate : new String[]{ "get" + cap, "is" + cap, name }) {
            try {
                Method m = type.getMethod(candidate);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new EvaluationException("Error accessing '" + name + "' on "
                        + type.getSimpleName() + ": " + e.getMessage(), e);
            }
        }

        // Public field fallback
        try {
            Field f = type.getField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
            throw new EvaluationException("Error accessing field '" + name + "': " + e.getMessage(), e);
        }

        throw new EvaluationException(
                "Cannot resolve property '" + name + "' on " + type.getSimpleName());
    }

    static Object invoke(Object target, String name, Object[] args) {
        if (target == null) throw new EvaluationException(
                "Cannot invoke '" + name + "' on null");

        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            try {
                m.setAccessible(true);
                return m.invoke(target, coerce(args, m.getParameterTypes()));
            } catch (Exception e) {
                throw new EvaluationException("Error invoking '" + name + "': " + e.getMessage(), e);
            }
        }
        throw new EvaluationException(
                "No method '" + name + "' with " + args.length + " arg(s) on "
                + target.getClass().getSimpleName());
    }

    /** Lightweight coercion: convert String → Number when needed. */
    private static Object[] coerce(Object[] args, Class<?>[] paramTypes) {
        Object[] result = args.clone();
        for (int i = 0; i < result.length; i++) {
            if (result[i] instanceof String s) {
                Class<?> p = paramTypes[i];
                if (p == int.class  || p == Integer.class) result[i] = Integer.parseInt(s);
                else if (p == long.class || p == Long.class)  result[i] = Long.parseLong(s);
                else if (p == boolean.class || p == Boolean.class) result[i] = Boolean.parseBoolean(s);
            }
        }
        return result;
    }
}