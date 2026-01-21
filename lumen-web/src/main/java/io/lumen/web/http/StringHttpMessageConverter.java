package io.lumen.web.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

class StringHttpMessageConverter implements HttpMessageConverter {

    @Override
    public boolean canRead(Class<?> clazz, String contentType) {
        return clazz == String.class && contentType != null && contentType.contains("text/plain");
    }

    @Override
    public Object read(Class<?> targetType, Type genericType, HttpServletRequest request) throws Exception {
        return new String(
                request.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    @Override
    public boolean canWrite(Class<?> clazz, String contentType) {
        return clazz == String.class &&
                (contentType == null || contentType.contains("text/plain"));
    }

    @Override
    public void write(Object object, Class<?> type, HttpServletResponse response) throws Exception {
        response.setContentType("text/plain;charset=UTF-8");
        String content = String.valueOf(object);
        response.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
    }
}
