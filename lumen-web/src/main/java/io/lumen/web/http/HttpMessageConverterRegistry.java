package io.lumen.web.http;

import io.lumen.context.annotation.Component;
import io.lumen.web.exception.HttpMessageConvertException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

@Component
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

    public void write(Object object, Class<?> type, HttpServletResponse response) {
        String contentType = response.getContentType();

        for (HttpMessageConverter converter : converters) {
            if (converter.canWrite(type, contentType)) {
                try {
                    converter.write(object, type, response);
                    return;
                } catch (Exception e) {
                    throw new HttpMessageConvertException("Conversion failed", e);
                }
            }
        }
        throw new HttpMessageConvertException(
                "No converter found for " + type.getSimpleName() + " to " + contentType
        );
    }

    public HttpMessageConverter findBestConverter(Class<?> clazz, String acceptHeader) {
        List<MediaType> requestedTypes = parseAcceptHeader(acceptHeader);

        for (MediaType requested : requestedTypes) {
            if (requested.getQuality() <= 0)
                continue;

            for (HttpMessageConverter converter : converters) {
                if (converter.canWrite(clazz, requested)) {
                    return converter;
                }
            }
        }
        return null;
    }

    private List<MediaType> parseAcceptHeader(String header) {
        if (header == null || header.isBlank()) {
            return List.of(MediaType.ALL);
        }
        return Arrays.stream(header.split(","))
                .map(MediaType::parse)
                .toList();
    }
}
