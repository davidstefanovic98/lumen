package io.lumen.web.http;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Type;

/**
 * An interface for converting HTTP messages to and from Java objects.
 */
interface HttpMessageConverter {

    /**
     * Checks if the converter can read the given class and content type.
     *
     * @param clazz       the target class
     * @param contentType the content type of the HTTP message
     * @return true if the converter can read, false otherwise
     */
    boolean canRead(Class<?> clazz, String contentType);

    /**
     * Reads the HTTP message and converts it to a Java object of the specified type.
     *
     * @param targetType  the target class
     * @param genericType the generic type
     * @param request     the HTTP servlet request
     * @return the converted Java object
     * @throws Exception if an error occurs during reading or conversion
     */
    Object read(
            Class<?> targetType,
            Type genericType,
            HttpServletRequest request
    ) throws Exception;
}
