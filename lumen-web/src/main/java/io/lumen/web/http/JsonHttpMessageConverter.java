package io.lumen.web.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.util.List;

class JsonHttpMessageConverter implements HttpMessageConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean canRead(Class<?> clazz, String contentType) {
        return contentType != null && contentType.contains("application/json");
    }

    @Override
    public Object read(Class<?> targetType, Type genericType, HttpServletRequest request) throws Exception {
        return objectMapper.readValue(request.getInputStream(), objectMapper.constructType(genericType));
    }

    @Override
    public boolean canWrite(Class<?> clazz, String contentType) {
        return clazz != String.class && clazz != byte[].class;
    }

    @Override
    public void write(Object object, Class<?> type, HttpServletResponse response) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getOutputStream(), object);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(MediaType.APPLICATION_OCTET_STREAM);
    }
}
