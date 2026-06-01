package io.lumen.gleam;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Default {@link EvaluationContext} implementation.
 * Variables, a root object, and named functions can all be registered at build time.
 */
public class StandardEvaluationContext implements EvaluationContext {

    private final Map<String, Object> variables = new HashMap<>();
    private final Map<String, Function<Object[], Object>> functions = new HashMap<>();
    private Object rootObject;

    public StandardEvaluationContext() {}

    public StandardEvaluationContext(Object rootObject) {
        this.rootObject = rootObject;
    }

    // ── builder-style mutators ────────────────────────────────────────────────

    public StandardEvaluationContext setVariable(String name, Object value) {
        variables.put(name, value);
        return this;
    }

    public StandardEvaluationContext setRootObject(Object root) {
        this.rootObject = root;
        return this;
    }

    /**
     * Registers a named function that expressions can call as {@code functionName(arg1, arg2)}.
     * The {@link Function} receives the evaluated arguments as an {@code Object[]} array.
     */
    public StandardEvaluationContext registerFunction(String name, Function<Object[], Object> fn) {
        functions.put(name, fn);
        return this;
    }

    // ── EvaluationContext ─────────────────────────────────────────────────────

    @Override
    public Object lookupVariable(String name) {
        return variables.get(name);
    }

    @Override
    public Object getRootObject() {
        return rootObject;
    }

    @Override
    public Object invokeFunction(String name, Object... args) {
        Function<Object[], Object> fn = functions.get(name);
        if (fn == null) throw new EvaluationException("Unknown function: '" + name + "'");
        return fn.apply(args);
    }

    @Override
    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }
}