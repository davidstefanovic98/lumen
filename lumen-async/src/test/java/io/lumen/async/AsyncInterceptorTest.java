package io.lumen.async;

import io.lumen.async.annotation.Async;
import io.lumen.core.proxy.ProxyFactory;
import io.lumen.core.task.TaskDecorator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class AsyncInterceptorTest {

    ExecutorService executor;
    AsyncUncaughtExceptionHandler noopHandler = (ex, method, args) -> {};
    Supplier<List<TaskDecorator>> noDecorators = List::of;

    @BeforeEach void setUp()    { executor = Executors.newFixedThreadPool(2); }
    @AfterEach  void tearDown() { executor.shutdownNow(); }

    static class WorkService {
        volatile String callerThread;
        volatile String workerThread;

        @Async
        public void fireAndForget() throws InterruptedException {
            workerThread = Thread.currentThread().getName();
            Thread.sleep(50);
        }

        @Async
        public CompletableFuture<String> computeAsync() {
            workerThread = Thread.currentThread().getName();
            return CompletableFuture.completedFuture("result");
        }

        public String syncMethod() {
            return "sync";
        }

        @Async
        public void throwingMethod() {
            throw new IllegalStateException("oops");
        }
    }

    private WorkService proxy(WorkService delegate) {
        return ProxyFactory.createDelegatingProxy(WorkService.class, delegate,
                List.of(new AsyncInterceptor(executor, noopHandler, noDecorators)));
    }

    // --- @Async void ---

    @Test
    void async_void_returnsImmediately() throws Exception {
        WorkService svc = proxy(new WorkService());
        long start = System.currentTimeMillis();
        svc.fireAndForget();
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 40, "fire-and-forget should return before the 50ms sleep completes");
    }

    @Test
    void async_void_runsOnDifferentThread() throws Exception {
        WorkService delegate = new WorkService();
        WorkService svc = proxy(delegate);
        String callerThread = Thread.currentThread().getName();

        svc.fireAndForget();
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertNotNull(delegate.workerThread);
        assertNotEquals(callerThread, delegate.workerThread,
                "@Async method must execute on a pool thread, not the caller's thread");
    }

    // --- @Async CompletableFuture ---

    @Test
    void async_future_returnsCompletableFuture() throws Exception {
        WorkService delegate = new WorkService();
        WorkService svc = proxy(delegate);

        Object result = svc.computeAsync();
        assertInstanceOf(CompletableFuture.class, result);
    }

    @Test
    void async_future_resolvesCorrectly() throws Exception {
        WorkService svc = proxy(new WorkService());
        CompletableFuture<?> future = (CompletableFuture<?>) svc.computeAsync();
        assertEquals("result", future.get(2, TimeUnit.SECONDS));
    }

    // --- non-@Async methods pass through ---

    @Test
    void nonAsync_method_executesOnCallerThread() {
        WorkService svc = proxy(new WorkService());
        assertEquals("sync", svc.syncMethod());
    }

    // --- exception handling ---

    @Test
    void async_void_uncaughtExceptionGoesToHandler() throws Exception {
        AtomicReference<Throwable> captured = new AtomicReference<>();
        AsyncUncaughtExceptionHandler handler = (ex, method, args) -> captured.set(ex);

        WorkService delegate = new WorkService();
        WorkService svc = ProxyFactory.createDelegatingProxy(WorkService.class, delegate,
                List.of(new AsyncInterceptor(executor, handler, noDecorators)));

        svc.throwingMethod();
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertNotNull(captured.get());
        assertInstanceOf(IllegalStateException.class, captured.get());
        assertEquals("oops", captured.get().getMessage());
    }

    @Test
    void async_future_exceptionCompletesExceptionally() throws Exception {
        WorkService delegate = new WorkService() {
            @Override @Async
            public CompletableFuture<String> computeAsync() {
                throw new RuntimeException("boom");
            }
        };
        WorkService svc = proxy(delegate);

        CompletableFuture<?> future = (CompletableFuture<?>) svc.computeAsync();
        assertTrue(future.isCompletedExceptionally() || future.exceptionally(e -> null).get() == null,
                "Future should complete exceptionally");
    }

    // --- AsyncProcessor integration ---

    @Test
    void asyncProcessor_wrapsBeansWithAsyncMethods() {
        WorkService original = new WorkService();
        AsyncProcessor processor = new AsyncProcessor(executor, noopHandler, noDecorators);
        Object result = processor.afterInstantiation(null, original);

        assertNotSame(original, result, "Bean with @Async methods should be proxied");
        assertInstanceOf(WorkService.class, result);
    }

    @Test
    void asyncProcessor_skipsBeansWithoutAsyncMethods() {
        Object plain = new Object() { public void doWork() {} };
        AsyncProcessor processor = new AsyncProcessor(executor, noopHandler, noDecorators);
        Object result = processor.afterInstantiation(null, plain);
        assertSame(plain, result, "Bean without @Async methods should not be proxied");
    }

    // --- TaskDecorator propagation ---

    /** A ThreadLocal-backed context to prove decorators capture caller state and restore it on the worker. */
    static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    static class ContextService {
        volatile String seenOnWorker;
        final CountDownLatch done = new CountDownLatch(1);

        @Async
        public void readContext() {
            seenOnWorker = CONTEXT.get();
            done.countDown();
        }
    }

    @Test
    void taskDecorator_propagatesCallerContextToWorkerThread() throws Exception {
        // Decorator captures CONTEXT on the calling thread, restores it on the worker thread.
        TaskDecorator contextPropagating = runnable -> {
            String captured = CONTEXT.get();
            return () -> {
                CONTEXT.set(captured);
                try { runnable.run(); }
                finally { CONTEXT.remove(); }
            };
        };

        ContextService delegate = new ContextService();
        ContextService svc = ProxyFactory.createDelegatingProxy(ContextService.class, delegate,
                List.of(new AsyncInterceptor(executor, noopHandler, () -> List.of(contextPropagating))));

        CONTEXT.set("caller-value");
        try {
            svc.readContext();
        } finally {
            CONTEXT.remove();
        }

        assertTrue(delegate.done.await(2, TimeUnit.SECONDS), "async task did not complete");
        assertEquals("caller-value", delegate.seenOnWorker,
                "decorator should propagate the caller's ThreadLocal value to the worker thread");
    }

    @Test
    void withoutDecorator_callerContextIsNotVisibleOnWorker() throws Exception {
        ContextService delegate = new ContextService();
        ContextService svc = ProxyFactory.createDelegatingProxy(ContextService.class, delegate,
                List.of(new AsyncInterceptor(executor, noopHandler, noDecorators)));

        CONTEXT.set("caller-value");
        try {
            svc.readContext();
        } finally {
            CONTEXT.remove();
        }

        assertTrue(delegate.done.await(2, TimeUnit.SECONDS), "async task did not complete");
        assertNull(delegate.seenOnWorker,
                "without a decorator, the worker thread must not see the caller's ThreadLocal");
    }
}