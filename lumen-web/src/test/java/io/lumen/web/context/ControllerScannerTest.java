package io.lumen.web.context;

import io.lumen.web.Route;
import io.lumen.web.RouteRegistry;
import io.lumen.web.annotation.GetMapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControllerScannerTest {

    static class BaseController {
        @GetMapping("/base")
        public String base() { return "base"; }
    }

    static class ChildController extends BaseController {
        @GetMapping("/child")
        public String child() { return "child"; }
    }

    static class OverridingController extends BaseController {
        @Override
        @GetMapping("/base")
        public String base() { return "overridden"; }
    }

    @Test
    void scanController_registersInheritedHandlerMethods() {
        RouteRegistry registry = new RouteRegistry();
        ChildController instance = new ChildController();

        ControllerScanner.scanController(instance, ChildController.class, registry);

        assertEquals(2, registry.getRouteCount());
        List<String> paths = registry.getAllRoutes().stream().map(Route::getPathPattern).toList();
        assertTrue(paths.contains("/base"), "inherited handler method should be registered");
        assertTrue(paths.contains("/child"), "declared handler method should be registered");
    }

    @Test
    void scanController_overriddenMethod_registeredOnce() {
        RouteRegistry registry = new RouteRegistry();
        OverridingController instance = new OverridingController();

        ControllerScanner.scanController(instance, OverridingController.class, registry);

        assertEquals(1, registry.getRouteCount(),
                "an overridden handler method should be registered once, not once per declaration");
    }
}