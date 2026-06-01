package io.lumen.context;

import io.lumen.core.component.Dependency;
import io.lumen.core.component.processor.DependencyProvider;
import io.lumen.core.context.Environment;
import io.lumen.gleam.Gleam;
import io.lumen.gleam.StandardEvaluationContext;

import static io.lumen.core.util.Utils.stripPlaceholder;

public class PropertyDependencyProvider implements DependencyProvider {

    private final Environment environment;

    public PropertyDependencyProvider(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object provide(Dependency dependency) {
        String key = dependency.getValueKey();
        if (key == null) throw new UnsupportedOperationException("Cannot provide dependency: " + dependency);

        // #{gleamExpression} — evaluate with Gleam; environment is root + variable
        if (key.startsWith("#{") && key.endsWith("}")) {
            String expr = key.substring(2, key.length() - 1);
            StandardEvaluationContext ctx = new StandardEvaluationContext(environment);
            ctx.setVariable("env", environment);
            Object result = Gleam.evaluate(expr, ctx);
            return coerce(result, dependency.getType());
        }

        // ${property.key} — property lookup (existing behaviour)
        return environment.getProperty(stripPlaceholder(key), dependency.getType());
    }

    @Override
    public boolean canProvide(Dependency dependency) {
        return dependency.getValueKey() != null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T coerce(Object value, Class<T> type) {
        if (value == null) return null;
        if (type == null || type.isInstance(value)) return (T) value;
        String s = value.toString();
        if (type == String.class)                        return type.cast(s);
        if (type == int.class || type == Integer.class)  return type.cast(Integer.parseInt(s));
        if (type == long.class || type == Long.class)    return type.cast(Long.parseLong(s));
        if (type == boolean.class || type == Boolean.class) return type.cast(Boolean.parseBoolean(s));
        if (type == double.class || type == Double.class) return type.cast(Double.parseDouble(s));
        return (T) value;
    }
}
