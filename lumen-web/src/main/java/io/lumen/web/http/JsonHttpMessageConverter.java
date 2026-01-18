package io.lumen.web.http;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

public class JsonHttpMessageConverter implements HttpMessageConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean canRead(Class<?> clazz, String contentType) {
        return contentType != null && contentType.contains("application/json");
    }

    @Override
    public Object read(Class<?> targetType, Type genericType, HttpServletRequest request) throws Exception {
        return objectMapper.readValue(request.getInputStream(), objectMapper.constructType(genericType));
    }
}
