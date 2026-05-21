package io.lumen.data.query;

import io.lumen.data.annotation.Modifying;
import io.lumen.data.annotation.Query;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class AnnotationQueryParser implements QueryParser {

    private static final Pattern NAMED_PARAM = Pattern.compile(":\\w+");

    @Override
    public QueryDescriptor parse(Method method, Class<?> entityClass) {
        Query ann = method.getAnnotation(Query.class);
        if (ann == null) return null;

        boolean modifying   = method.isAnnotationPresent(Modifying.class);
        boolean namedParams = NAMED_PARAM.matcher(ann.value()).find();
        String countQuery   = ann.countQuery().isEmpty() ? null : ann.countQuery();

        return new QueryDescriptor(
                ann.value(),
                ann.nativeQuery(),
                modifying,
                QueryDescriptor.QueryType.SELECT,
                namedParams,
                countQuery
        );
    }
}
