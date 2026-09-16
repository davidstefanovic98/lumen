package io.lumen.web.exception.handle;

import io.lumen.web.annotation.ExceptionHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControllerAdviceRegistryTest {

    static class BaseAdvice {
        @ExceptionHandler(IllegalStateException.class)
        public void handleIllegalState() {}
    }

    static class ChildAdvice extends BaseAdvice {
        @ExceptionHandler(IllegalArgumentException.class)
        public void handleIllegalArgument() {}
    }

    @Test
    void registerAdvice_registersInheritedExceptionHandlerMethods() {
        var registry = new ControllerAdviceRegistry();

        registry.registerAdvice(new ChildAdvice());

        assertEquals(2, registry.getCount());
        assertNotNull(registry.findHandler(new IllegalStateException()),
                "inherited @ExceptionHandler method should be registered");
        assertNotNull(registry.findHandler(new IllegalArgumentException()),
                "declared @ExceptionHandler method should be registered");
    }
}