package io.lumen.web.argument;

import io.lumen.web.annotation.RequestBody;
import io.lumen.web.exception.MethodArgumentNotValidException;
import io.lumen.web.http.ErrorResponse.FieldError;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestBodyArgumentResolverTest {

    record UserDto(String name, int age) {}

    @SuppressWarnings("unused")
    static void handler(@RequestBody UserDto user) {}

    @SuppressWarnings("unused")
    static void plainHandler(String s) {}

    private Parameter param(String name, Class<?> type) throws Exception {
        return RequestBodyArgumentResolverTest.class.getDeclaredMethod(name, type).getParameters()[0];
    }

    private HttpServletRequest jsonRequest(String json) throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getContentType()).thenReturn("application/json");
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(req.getInputStream()).thenReturn(new ServletInputStream() {
            private final InputStream is = new ByteArrayInputStream(bytes);
            @Override public int read() throws java.io.IOException { return is.read(); }
            @Override public boolean isFinished() { return false; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener r) {}
        });
        return req;
    }

    @Test
    void supports_paramAnnotatedWithRequestBody() throws Exception {
        RequestBodyArgumentResolver resolver = new RequestBodyArgumentResolver(new HttpMessageConverterRegistry(), List.of());
        assertTrue(resolver.supports(param("handler", UserDto.class)));
    }

    @Test
    void supports_unannotatedParam_false() throws Exception {
        RequestBodyArgumentResolver resolver = new RequestBodyArgumentResolver(new HttpMessageConverterRegistry(), List.of());
        assertFalse(resolver.supports(param("plainHandler", String.class)));
    }

    @Test
    void resolve_deserializesJsonBody() throws Exception {
        RequestBodyArgumentResolver resolver = new RequestBodyArgumentResolver(new HttpMessageConverterRegistry(), List.of());
        Parameter p = param("handler", UserDto.class);
        HttpServletRequest req = jsonRequest("{\"name\":\"Alice\",\"age\":30}");

        Object result = resolver.resolve(p, req, mock(HttpServletResponse.class), Map.of());

        assertInstanceOf(UserDto.class, result);
        UserDto dto = (UserDto) result;
        assertEquals("Alice", dto.name());
        assertEquals(30, dto.age());
    }

    @Test
    void resolve_appliesEveryValidatorToTheBody_withItsParameter() throws Exception {
        List<String> calls = new ArrayList<>();
        Parameter p = param("handler", UserDto.class);
        RequestBodyArgumentResolver resolver = new RequestBodyArgumentResolver(new HttpMessageConverterRegistry(), List.of(
                (body, parameter) -> calls.add("first:" + ((UserDto) body).name() + ":" + (parameter == p)),
                (body, parameter) -> calls.add("second")));

        resolver.resolve(p, jsonRequest("{\"name\":\"Alice\",\"age\":30}"), mock(HttpServletResponse.class), Map.of());

        assertEquals(List.of("first:Alice:true", "second"), calls);
    }

    @Test
    void resolve_validatorFailure_propagatesUnwrapped() throws Exception {
        MethodArgumentNotValidException invalid = new MethodArgumentNotValidException(
                List.of(new FieldError("name", "must not be blank")), null);
        RequestBodyArgumentResolver resolver = new RequestBodyArgumentResolver(new HttpMessageConverterRegistry(),
                List.of((body, parameter) -> { throw invalid; }));

        MethodArgumentNotValidException thrown = assertThrows(MethodArgumentNotValidException.class, () ->
                resolver.resolve(param("handler", UserDto.class), jsonRequest("{\"name\":\"\",\"age\":30}"),
                        mock(HttpServletResponse.class), Map.of()));
        assertSame(invalid, thrown);
    }

    @Test
    void resolve_nullBody_skipsValidators() throws Exception {
        RequestBodyArgumentResolver resolver = new RequestBodyArgumentResolver(new HttpMessageConverterRegistry(),
                List.of((body, parameter) -> fail("validator must not run for a null body")));

        assertNull(resolver.resolve(param("handler", UserDto.class), jsonRequest("null"),
                mock(HttpServletResponse.class), Map.of()));
    }
}
