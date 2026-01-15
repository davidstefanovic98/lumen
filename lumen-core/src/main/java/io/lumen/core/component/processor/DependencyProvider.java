package io.lumen.core.component.processor;

import io.lumen.core.component.Dependency;

/**
 * Interface for providing non-LIGHT dependencies (e.g., @Value, environment variables).
 * This is the extension point for external dependency resolution.
 * lumen-core provides a no-op implementation.
 * lumen-context can provide property/environment resolution.
 */
public interface DependencyProvider {
    /**
     * Provide a value for the given dependency.
     *
     * @param dependency the dependency to resolve
     * @return the resolved value
     * @throws UnsupportedOperationException if this provider cannot handle the dependency
     */
    Object provide(Dependency dependency);

    /**
     * Check if this provider can handle the given dependency.
     *
     * @param dependency the dependency to check
     * @return true if this provider can resolve it
     */
    boolean canProvide(Dependency dependency);
}