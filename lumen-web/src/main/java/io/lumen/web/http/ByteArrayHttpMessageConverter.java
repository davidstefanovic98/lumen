package io.lumen.web.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;

class ByteArrayHttpMessageConverter implements HttpMessageConverter {

    @Override
    public boolean canRead(Class<?> clazz, String contentType) {
        return clazz == byte[].class && contentType != null && contentType.contains("application/octet-stream");
    }

    @Override
    public Object read(Class<?> targetType, Type genericType, HttpServletRequest request) throws Exception {
        return request.getInputStream().readAllBytes();
    }

    @Override
    public boolean canWrite(Class<?> clazz, String contentType) {
        return clazz == byte[].class;
    }

    @Override
    public void write(Object object, Class<?> type, HttpServletResponse response) throws Exception {
        byte[] bytes = (byte[]) object;
        response.setContentType("application/octet-stream");
        response.setContentLength(bytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        }
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(MediaType.APPLICATION_OCTET_STREAM);
    }
}
