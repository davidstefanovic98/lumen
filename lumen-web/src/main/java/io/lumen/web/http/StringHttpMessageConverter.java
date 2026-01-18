package io.lumen.web.http;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

class StringHttpMessageConverter implements HttpMessageConverter {

    @Override
    public boolean canRead(Class<?> clazz, String contentType) {
        return clazz == String.class;
    }

    @Override
    public Object read(Class<?> targetType, Type genericType, HttpServletRequest request) throws Exception {
        return new String(
                request.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }
}
