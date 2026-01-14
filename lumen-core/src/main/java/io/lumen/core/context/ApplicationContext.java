package io.lumen.core.context;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightFactory;

/**
 * Represents a basic <b>Application Context</b> for the lumen-core DI container.
 * <p>
 * This interface defines the contract for a lightweight dependency injection container
 * that can register classes, instances, and factories, resolve dependencies,
 * and initialize the object graph.
 * <p>
 * This context supports:
 * <ul>
 *     <li>Manual registration of classes.</li>
 *     <li>Manual registration of pre-created instances.</li>
 *     <li>Factory-based creation of beans.</li>
 *     <li>Constructor-based dependency injection.</li>
 *     <li>Type-based dependency resolution (with optional named instances).</li>
 * </ul>
 */
public interface ApplicationContext {

    /**
     * Register a class type in the container.
     * The container will instantiate this class when needed and inject
     * its constructor dependencies automatically.
     *
     * @param clazz the class to register
     * @param <T>   the type of the class
     */
    <T> void register(Class<T> clazz);

    /**
     * Retrieve an instance of the specified type from the container.
     * If the type was registered, its dependencies will have been
     * automatically resolved and injected.
     *
     * @param clazz the class type to retrieve
     * @param <T>   the type of the class
     * @return the resolved instance of the class
     * @throws RuntimeException if the type has not been registered
     *                          or if dependency resolution fails
     */
    <T> T getLight(Class<T> clazz);


    <T> T getLight(String name);

    /**
     * Initialize the container.
     * <p>
     * This triggers the resolution and instantiation of all registered classes,
     * factories, and instances. All constructor dependencies will be recursively
     * resolved. Circular dependencies will be detected and reported.
     */
    void initialize();

    /**
     * Register a pre-created instance in the container with a specific name.
     * This allows the container to inject this instance into other classes
     * without creating a new object.
     *
     * @param name     the unique name of the instance
     * @param instance the pre-created instance to register
     * @param <T>      the type of the instance
     */
    <T> void registerInstance(String name, T instance);

    /**
     * Register a factory that can create instances of a given type.
     * <p>
     * Factories are useful when you want to control the creation logic of a bean,
     * for example to configure it before returning or to wrap third-party objects.
     *
     * @param name    the unique name of the factory bean
     * @param type    the class type that this factory produces
     * @param factory the factory that creates instances
     */
    void registerFactory(String name, Class<?> type, LightFactory factory);

    LightContainer getLightContainer();
}
