package io.lumen.web.argument;

import io.lumen.web.annotation.PathVariable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Parameter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PathVariableArgumentResolverTest {

    private final PathVariableArgumentResolver resolver = new PathVariableArgumentResolver();

    // Helper methods whose parameters we'll reflect over
    @SuppressWarnings("unused")
    static void methodWithAnnotatedName(@PathVariable("userId") Long userId) {}

    @SuppressWarnings("unused")
    static void methodWithNoAnnotationValue(@PathVariable Long id) {}

    private Parameter param(String methodName, Class<?> type) throws Exception {
        return PathVariableArgumentResolverTest.class
                .getDeclaredMethod(methodName, type)
                .getParameters()[0];
    }

    @Test
    void supports_parameterAnnotatedWithPathVariable() throws Exception {
        assertTrue(resolver.supports(param("methodWithAnnotatedName", Long.class)));
    }

    @Test
    void supports_plainParameter_false() throws Exception {
        Parameter p = String.class.getMethod("charAt", int.class).getParameters()[0];
        assertFalse(resolver.supports(p));
    }

    @Test
    void resolve_usesAnnotationValueAsKeyName() throws Exception {
        Parameter p = param("methodWithAnnotatedName", Long.class);
        Object result = resolver.resolve(p, mock(jakarta.servlet.http.HttpServletRequest.class),
                mock(jakarta.servlet.http.HttpServletResponse.class),
                Map.of("userId", "99"));
        assertEquals(99L, result);
    }

    @Test
    void resolve_usesParameterNameWhenAnnotationValueBlank() throws Exception {
        Parameter p = param("methodWithNoAnnotationValue", Long.class);
        Object result = resolver.resolve(p, mock(jakarta.servlet.http.HttpServletRequest.class),
                mock(jakarta.servlet.http.HttpServletResponse.class),
                Map.of("id", "7"));
        assertEquals(7L, result);
    }
}