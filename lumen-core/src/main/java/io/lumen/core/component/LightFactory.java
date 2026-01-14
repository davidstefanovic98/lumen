package io.lumen.core.component;

/**
 * Factory interface for creating custom light instances.
 * This allows users to provide their own creation logic.
 */
@FunctionalInterface
public interface LightFactory {
    /**
     * Create an instance based on the light definition.
     *
     * @param definition the definition of what to create
     * @return the created instance
     */
    Object create(LightDefinition definition);
}