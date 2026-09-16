package io.lumen.web;

import io.lumen.web.annotation.PathVariable;
import io.lumen.web.exception.AmbiguousMappingException;
import io.lumen.web.exception.PathVariableNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

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
}