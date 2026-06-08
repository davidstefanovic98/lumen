package io.lumen.cache;

import io.lumen.gleam.Gleam;
import io.lumen.gleam.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public class SimpleKeyGenerator implements CacheKeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String prefix = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        if (params.length == 0)
            return prefix;
        if (params.length == 1)
            return prefix + ":" + params[0];
        return prefix + ":" + Arrays.toString(params);
    }

    /**
     * Resolves a cache key expression against the method invocation.
     * <ul>
     *   <li>Blank → {@link #generate(Object, Method, Object...)}</li>
     *   <li>Starts with {@code #} → evaluated as a Gleam expression with all
     *       parameters bound as variables ({@code #paramName}) and the target
     *       bean as the root object. Supports chains: {@code #product.id}.</li>
     *   <li>Anything else → used as a literal string key.</li>
     * </ul>
     */
    public Object resolveKey(String expression, Method method, Object target, Object[] args) {
        if (expression == null || expression.isBlank()) {
            return generate(target, method, args);
        }
        if (expression.startsWith("#")) {
            StandardEvaluationContext ctx = new StandardEvaluationContext(target);
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length && i < args.length; i++) {
                ctx.setVariable(params[i].getName(), args[i]);
            }
            return Gleam.evaluate(expression, ctx);
        }
        return expression;
    }
}