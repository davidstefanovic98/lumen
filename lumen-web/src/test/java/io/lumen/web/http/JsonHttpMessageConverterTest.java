package io.lumen.web.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonHttpMessageConverterTest {

    private record Payload(String name) {}

    private final JsonHttpMessageConverter converter = new JsonHttpMessageConverter();

    @Test
    void getSupportedMediaTypes_reportsApplicationJson() {
        assertEquals(java.util.List.of(MediaType.APPLICATION_JSON), converter.getSupportedMediaTypes());
    }

    @Test
    void canWrite_withApplicationJsonMediaType_returnsTrue() {
        assertTrue(converter.canWrite(Payload.class, MediaType.APPLICATION_JSON));
    }

    @Test
    void canWrite_withOctetStreamMediaType_returnsFalse() {
        assertFalse(converter.canWrite(Payload.class, MediaType.APPLICATION_OCTET_STREAM));
    }
}