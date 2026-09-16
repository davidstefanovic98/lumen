package io.lumen.web.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpMessageConverterRegistryTest {

    private record Payload(String name) {}

    private final HttpMessageConverterRegistry registry = new HttpMessageConverterRegistry();

    @Test
    void findBestConverter_acceptApplicationJson_forPojo_resolvesToJsonConverter() {
        HttpMessageConverter best = registry.findBestConverter(Payload.class, "application/json");

        assertNotNull(best, "an explicit Accept: application/json request for a POJO should resolve to a converter");
        assertEquals(JsonHttpMessageConverter.class, best.getClass());
    }

    @Test
    void findBestConverter_acceptAll_forPojo_resolvesToJsonConverter() {
        HttpMessageConverter best = registry.findBestConverter(Payload.class, "*/*");

        assertNotNull(best);
        assertEquals(JsonHttpMessageConverter.class, best.getClass());
    }

    @Test
    void findBestConverter_acceptTextPlain_forString_resolvesToStringConverter() {
        HttpMessageConverter best = registry.findBestConverter(String.class, "text/plain");

        assertNotNull(best);
        assertEquals(StringHttpMessageConverter.class, best.getClass());
    }
}