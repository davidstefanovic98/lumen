package io.lumen.data.query;

import java.lang.reflect.Method;
import io.lumen.data.annotation.Query;

public class AnnotationQueryParser implements QueryParser{
    @Override
    public QueryDescriptor parse(Method method, Class<?> entityClass) {
        Query ann = method.getAnnotation(Query.class);
        if (ann != null) {
            return new QueryDescriptor(ann.value(), ann.nativeQuery());
        }
        return null;
    }
}
