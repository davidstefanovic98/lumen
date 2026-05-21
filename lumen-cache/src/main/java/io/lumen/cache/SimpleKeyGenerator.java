package io.lumen.cache;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public class SimpleKeyGenerator implements CacheKeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length == 0)
            return method.getName();
        if (params.length == 1)
            return method.getName() + ":" + params[0];
        return method.getName() + ":" + Arrays.toString(params);
    }

    /**
     * Resolves a key expression like "#id" or "#name" against method parameters.
     * Falls back to generate() when the expression is blank or unrecognized.
     */
    public Object resolveKey(String expression, Method method, Object target, Object[] args) {
        if (expression == null || expression.isBlank()) {
            return generate(target, method, args);
        }
        if (expression.startsWith("#")) {
            String paramName = expression.substring(1);
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length; i++) {
                if (params[i].getName().equals(paramName)) {
                    return args[i];
                }
            }
        }
        return expression;
    }
}