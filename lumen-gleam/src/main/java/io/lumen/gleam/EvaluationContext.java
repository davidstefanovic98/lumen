package io.lumen.gleam;

/**
 * Carries the runtime state needed to evaluate an {@link Expression}:
 * named variables, a root object for bare property access, and
 * registered functions that expressions can call by name.
 */
public interface EvaluationContext {

    /** Returns the value of a named variable ({@code #name}), or {@code null} if unset. */
    Object lookupVariable(String name);

    /**
     * Returns the root object against which bare property names are resolved.
     * May be {@code null} if no root is set.
     */
    Object getRootObject();

    /**
     * Invokes a registered function by name with the supplied arguments.
     * @throws EvaluationException if the function is not registered
     */
    Object invokeFunction(String name, Object... args);

    /** Returns {@code true} if a function with the given name is registered. */
    boolean hasFunction(String name);
}