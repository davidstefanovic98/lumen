package io.lumen.core.task;

/**
 * Decorates a {@link Runnable} before it is handed to an executor, allowing
 * thread-bound context (security, logging MDC, tenant, etc.) to be captured on the
 * submitting thread and re-established on the worker thread.
 *
 * <p>Implementations are discovered as beans by {@code lumen-async} and applied to every
 * task submitted by {@code @Async}. {@link #decorate(Runnable)} is invoked on the
 * <em>calling</em> thread — capture any {@code ThreadLocal} state there, and restore it
 * inside the returned {@code Runnable}, which runs on the <em>worker</em> thread.
 *
 * <pre>{@code
 * public Runnable decorate(Runnable runnable) {
 *     var captured = SomeContextHolder.get();      // calling thread
 *     return () -> {
 *         var previous = SomeContextHolder.get();
 *         SomeContextHolder.set(captured);
 *         try { runnable.run(); }                  // worker thread
 *         finally { SomeContextHolder.set(previous); }
 *     };
 * }
 * }</pre>
 *
 * <p>Register an implementation as a {@code @Light}/{@code @Component} bean, or via
 * {@code container.registerExternalInstance(...)} from a {@code LumenModule}.
 */
@FunctionalInterface
public interface TaskDecorator {

    /**
     * Wraps the given runnable. Called on the submitting thread.
     *
     * @param runnable the task to decorate
     * @return the decorated task, run later on a worker thread
     */
    Runnable decorate(Runnable runnable);
}