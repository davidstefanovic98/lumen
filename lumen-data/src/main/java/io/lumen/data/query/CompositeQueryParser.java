package io.lumen.data.query;

import java.lang.reflect.Method;
import java.util.List;

public class CompositeQueryParser implements QueryParser {
    private final List<QueryParser> parsers;

    public CompositeQueryParser() {
        this.parsers = List.of(
                new AnnotationQueryParser(),
                new NameResolvingQueryParser()
        );
    }

    @Override
    public QueryDescriptor parse(Method method, Class<?> entityClass) {
        for (QueryParser parser : parsers) {
            QueryDescriptor jpql = parser.parse(method, entityClass);
            if (jpql != null) {
                return jpql;
            }
        }
        throw new UnsupportedOperationException(
                "Could not resolve query for method: " + method.getName()
        );
    }
}
