package io.lumen.web.http;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Type;

class ByteArrayHttpMessageConverter implements HttpMessageConverter {

    @Override
    public boolean canRead(Class<?> clazz, String contentType) {
        return clazz == byte[].class;
    }

    @Override
    public Object read(Class<?> targetType, Type genericType, HttpServletRequest request) throws Exception {
        return request.getInputStream().readAllBytes();
    }
}
