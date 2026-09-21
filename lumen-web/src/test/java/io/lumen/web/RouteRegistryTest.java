package io.lumen.web;

import io.lumen.web.annotation.PathVariable;
import io.lumen.web.exception.AmbiguousMappingException;
import io.lumen.web.exception.PathVariableNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RouteRegistryTest {

    RouteRegistry registry;

    static class FakeController {
        public void handle() {}
        public void other() {}
        public void withMatchingPathVariable(@PathVariable("id") Long id) {}
        public void withMismatchedPathVariable(@PathVariable("userId") Long userId) {}
    }

    Method handleMethod;
    Object controller;

    @BeforeEach
    void setUp() throws Exception {
        registry = new RouteRegistry();
        controller = new FakeController();
        handleMethod = FakeController.class.getMethod("handle");
    }

    private Route route(String httpMethod, String pattern) {
        return new Route(controller, handleMethod, httpMethod, pattern);
    }

    // --- register and count ---

    @Test
    void registerAddsRoute() {
        registry.register(route("GET", "/users"));
        assertEquals(1, registry.getRouteCount());
    }

    // --- findMatch ---

    @Test
    void findMatch_exactPath_returnsMatch() {
        registry.register(route("GET", "/users"));
        RouteMatch match = registry.findMatch("/users", "GET");
        assertNotNull(match);
        assertEquals("/users", match.route().getPathPattern());
    }

    @Test
    void findMatch_wrongMethod_returnsNull() {
        registry.register(route("GET", "/users"));
        assertNull(registry.findMatch("/users", "POST"));
    }

    @Test
    void findMatch_unknownPath_returnsNull() {
        registry.register(route("GET", "/users"));
        assertNull(registry.findMatch("/posts", "GET"));
    }

    @Test
    void findMatch_withPathVariable_extractsVariable() {
        registry.register(route("GET", "/users/{id}"));
        RouteMatch match = registry.findMatch("/users/42", "GET");
        assertNotNull(match);
        assertEquals("42", match.pathVariables().get("id"));
    }

    @Test
    void findMatch_literalBeatsVariable_forSameRequest() throws Exception {
        Method other = FakeController.class.getMethod("other");
        Route literal  = new Route(controller, other,        "GET", "/users/profile");
        Route variable = new Route(controller, handleMethod, "GET", "/users/{id}");
        registry.register(variable);
        registry.register(literal);

        RouteMatch match = registry.findMatch("/users/profile", "GET");
        assertNotNull(match);
        assertEquals("/users/profile", match.route().getPathPattern());
    }

    // --- ambiguous mapping detection ---

    @Test
    void registerDuplicateExactRoute_throwsAmbiguousMappingException() {
        registry.register(route("GET", "/users"));
        assertThrows(AmbiguousMappingException.class, () -> registry.register(route("GET", "/users")));
    }

    @Test
    void registerTwoVariableRoutesWithSameStructure_throwsAmbiguousMappingException() {
        registry.register(route("GET", "/users/{id}"));
        assertThrows(AmbiguousMappingException.class, () -> registry.register(route("GET", "/users/{name}")));
    }

    @Test
    void registerSamePatternDifferentMethod_ok() {
        registry.register(route("GET", "/users"));
        assertDoesNotThrow(() -> registry.register(route("POST", "/users")));
    }

    // --- path variable / route pattern consistency ---

    @Test
    void registerRoute_pathVariableMatchesPatternSegment_ok() throws Exception {
        Method m = FakeController.class.getMethod("withMatchingPathVariable", Long.class);
        assertDoesNotThrow(() -> registry.register(new Route(controller, m, "GET", "/users/{id}")));
    }

    @Test
    void registerRoute_pathVariableHasNoMatchingPatternSegment_throwsAtRegistration() throws Exception {
        Method m = FakeController.class.getMethod("withMismatchedPathVariable", Long.class);
        assertThrows(PathVariableNotFoundException.class,
                () -> registry.register(new Route(controller, m, "GET", "/users/{id}")));
    }

    // --- exception messages show the real class, not a ByteBuddy proxy's generated name ---

    // Named to reproduce ByteBuddy's actual generated-subclass naming ("$ByteBuddy$<random>"),
    // simulating a controller wrapped by e.g. @Transactional/@PreAuthorize by the time routes
    // are scanned, without needing a real ByteBuddy dependency in this test.
    static class FakeController$ByteBuddy$fakeproxy extends FakeController {}

    @Test
    void ambiguousMappingMessage_usesRealControllerClassName_notProxyName() throws Exception {
        Object proxiedController = new FakeController$ByteBuddy$fakeproxy();
        Method m = FakeController.class.getMethod("handle");
        registry.register(new Route(proxiedController, m, "GET", "/users"));

        var thrown = assertThrows(AmbiguousMappingException.class,
                () -> registry.register(new Route(proxiedController, m, "GET", "/users")));

        assertTrue(thrown.getMessage().contains("FakeController#"), thrown.getMessage());
        assertFalse(thrown.getMessage().contains("ByteBuddy"), thrown.getMessage());
    }

    @Test
    void pathVariableNotFoundMessage_usesRealControllerClassName_notProxyName() throws Exception {
        Object proxiedController = new FakeController$ByteBuddy$fakeproxy();
        Method m = FakeController.class.getMethod("withMismatchedPathVariable", Long.class);

        var thrown = assertThrows(PathVariableNotFoundException.class,
                () -> registry.register(new Route(proxiedController, m, "GET", "/users/{id}")));

        assertTrue(thrown.getMessage().contains("FakeController#"), thrown.getMessage());
        assertFalse(thrown.getMessage().contains("ByteBuddy"), thrown.getMessage());
    }

    // --- concurrency: register() vs. findMatch() on the routing hot path ---

    @Test
    @Timeout(10)
    void concurrentRegisterAndFindMatch_neverThrowsAndRegistersAllRoutes() throws Exception {
        int routeCount = 200;
        int readerThreads = 8;

        ExecutorService writer = Executors.newSingleThreadExecutor();
        ExecutorService readers = Executors.newFixedThreadPool(readerThreads);
        AtomicBoolean stopReaders = new AtomicBoolean(false);
        AtomicInteger readerFailures = new AtomicInteger(0);
        CountDownLatch readersStarted = new CountDownLatch(readerThreads);

        for (int i = 0; i < readerThreads; i++) {
            readers.submit(() -> {
                readersStarted.countDown();
                try {
                    // findMatch() must tolerate concurrent register() calls without throwing
                    // ConcurrentModificationException or any other exception off a plain ArrayList.
                    while (!stopReaders.get()) {
                        registry.findMatch("/route-0", "GET");
                    }
                } catch (Exception e) {
                    readerFailures.incrementAndGet();
                }
            });
        }

        readersStarted.await();
        writer.submit(() -> {
            for (int i = 0; i < routeCount; i++) {
                registry.register(route("GET", "/route-" + i));
            }
        }).get(5, TimeUnit.SECONDS);

        stopReaders.set(true);
        readers.shutdown();
        assertTrue(readers.awaitTermination(5, TimeUnit.SECONDS));
        writer.shutdown();

        assertEquals(0, readerFailures.get(), "findMatch() must not throw while register() is mutating the list concurrently");
        assertEquals(routeCount, registry.getRouteCount());
    }

    @Test
    @Timeout(10)
    void concurrentRegisterDistinctRoutes_registersAllWithoutLostUpdates() throws Exception {
        int threadCount = 16;
        int routesPerThread = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);

        for (int t = 0; t < threadCount; t++) {
            int threadIndex = t;
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < routesPerThread; i++) {
                    registry.register(route("GET", "/thread-" + threadIndex + "-route-" + i));
                }
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        // synchronized register() must serialize the check-then-add so no route is lost or
        // double-counted even when every thread hits ambiguity checking at the same time.
        assertEquals(threadCount * routesPerThread, registry.getRouteCount());
    }
}