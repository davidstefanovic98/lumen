package io.lumen.core;

/**
 * Symmetric counterpart to {@link LumenInitializer}: a light that needs to release resources
 * (executors, connections, ...) on clean application shutdown.
 *
 * <p>Register as an external light (matching the {@code LumenInitializer} discovery pattern),
 * keyed by the concrete implementing class. Every registered {@code LumenDisposable} is invoked
 * once, from {@code AnnotationWebApplicationContext.stop()} - which only ever runs after a
 * successful startup (its shutdown hook is registered after the application has started, and an
 * explicit {@code .stop()} call implies the same). A module should never register its own raw
 * {@code Runtime.getRuntime().addShutdownHook(...)}: doing so runs on <em>any</em> JVM exit,
 * including a failed startup, which is how the async module's shutdown log used to print after
 * the startup-failure banner instead of not printing at all.
 */
public interface LumenDisposable {
    void onShutdown();
}