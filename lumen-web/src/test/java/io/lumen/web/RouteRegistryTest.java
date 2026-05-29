package io.lumen.web;

import io.lumen.web.exception.AmbiguousMappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class RouteRegistryTest {

    RouteRegistry registry;

    static class FakeController {
        public void handle() {}
        public void other() {}
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
}