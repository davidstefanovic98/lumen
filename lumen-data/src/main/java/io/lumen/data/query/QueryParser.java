package io.lumen.data.query;

import java.lang.reflect.Method;

public interface QueryParser {

    /**
     * Parse the query from the method and entity class.
     * @param method - the method to parse
     * @param entityClass - the entity class
     * @return A JPQL string or a prepared Query object
     */
    QueryDescriptor parse(Method method, Class<?> entityClass);
}
