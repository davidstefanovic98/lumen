package io.lumen.web.http;

import io.lumen.web.exception.HttpMessageConvertException;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Type;
import java.util.List;

public class HttpMessageConverterRegistry {

    private final List<HttpMessageConverter> converters;

    public HttpMessageConverterRegistry() {
        converters = List.of(
                new ByteArrayHttpMessageConverter(),
                new StringHttpMessageConverter(),
                new JsonHttpMessageConverter()
        );
    }

    public Object read(
            Class<?> targetType,
            Type genericType,
            HttpServletRequest request
    ) {
        String contentType = request.getContentType();

        for (HttpMessageConverter converter : converters) {
            if (converter.canRead(targetType, contentType)) {
                try {
                    return converter.read(targetType, genericType, request);
                } catch (Exception e) {
                    throw new HttpMessageConvertException(
                            "Failed to read request body using " +
                                    converter.getClass().getSimpleName(), e
                    );
                }
            }
        }

        throw new HttpMessageConvertException(
                "No HttpMessageConverter found for type " + targetType.getName() +
                        " and content-type " + contentType
        );
    }
}
