package io.lumen.web.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObjectBinderTest {

    public static class BaseDto {
        public String id;
    }

    public static class ChildDto extends BaseDto {
        public String name;
    }

    private HttpServletRequest requestWithParams(java.util.Map<String, String> params) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        params.forEach((k, v) -> when(req.getParameter(k)).thenReturn(v));
        return req;
    }

    @Test
    void bind_populatesFieldsInheritedFromSuperclass() {
        var request = requestWithParams(java.util.Map.of("id", "123", "name", "bob"));

        ChildDto result = (ChildDto) ObjectBinder.bind(ChildDto.class, request);

        assertEquals("123", result.id, "field declared on the DTO superclass should be bound");
        assertEquals("bob", result.name, "field declared on the DTO itself should be bound");
    }
}