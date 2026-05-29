package io.lumen.web.argument;

import io.lumen.web.annotation.RequestParam;
import io.lumen.web.exception.MissingRequestParameterException;
import io.lumen.web.exception.PrimitiveTypeRequestParameterException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Parameter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestParamArgumentResolverTest {

    private final RequestParamArgumentResolver resolver = new RequestParamArgumentResolver();

    @SuppressWarnings("unused")
    static void withRequired(@RequestParam("q") String q) {}

    @SuppressWarnings("unused")
    static void withOptional(@RequestParam(value = "page", required = false) String page) {}

    @SuppressWarnings("unused")
    static void withDefault(@RequestParam(value = "size", defaultValue = "10") String size) {}

    @SuppressWarnings("unused")
    static void withPrimitive(@RequestParam(value = "count", required = false) int count) {}

    @SuppressWarnings("unused")
    static void withIntegerWrapper(@RequestParam(value = "n", required = false) Integer n) {}

    private Parameter param(String name, Class<?> type) throws Exception {
        return RequestParamArgumentResolverTest.class.getDeclaredMethod(name, type).getParameters()[0];
    }

    private HttpServletRequest requestWithParam(String name, String value) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getParameter(name)).thenReturn(value);
        return req;
    }

    private HttpServletResponse response() {
        return mock(HttpServletResponse.class);
    }

    @Test
    void resolve_requiredParamPresent() throws Exception {
        Parameter p = param("withRequired", String.class);
        Object result = resolver.resolve(p, requestWithParam("q", "hello"), response(), Map.of());
        assertEquals("hello", result);
    }

    @Test
    void resolve_requiredParamMissing_throws() throws Exception {
        Parameter p = param("withRequired", String.class);
        assertThrows(MissingRequestParameterException.class,
                () -> resolver.resolve(p, requestWithParam("q", null), response(), Map.of()));
    }

    @Test
    void resolve_optionalParamMissing_returnsNull() throws Exception {
        Parameter p = param("withOptional", String.class);
        Object result = resolver.resolve(p, requestWithParam("page", null), response(), Map.of());
        assertNull(result);
    }

    @Test
    void resolve_defaultValue_usedWhenParamAbsent() throws Exception {
        Parameter p = param("withDefault", String.class);
        Object result = resolver.resolve(p, requestWithParam("size", null), response(), Map.of());
        assertEquals("10", result);
    }

    @Test
    void resolve_primitiveOptionalMissing_throws() throws Exception {
        Parameter p = param("withPrimitive", int.class);
        assertThrows(PrimitiveTypeRequestParameterException.class,
                () -> resolver.resolve(p, requestWithParam("count", null), response(), Map.of()));
    }

    @Test
    void resolve_wrapperOptionalMissing_returnsNull() throws Exception {
        Parameter p = param("withIntegerWrapper", Integer.class);
        Object result = resolver.resolve(p, requestWithParam("n", null), response(), Map.of());
        assertNull(result);
    }
}