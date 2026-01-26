package io.lumen.data.query;

import io.lumen.data.exception.PropertyResolveException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class NameResolvingQueryParser implements QueryParser {

    @Override
    public QueryDescriptor parse(Method method, Class<?> entityClass) {
        String methodName = method.getName();

        // 1. Extract the predicate after "By"
        int byIndex = methodName.indexOf("By");
        if (byIndex == -1) {
            throw new IllegalArgumentException("Method name must contain 'By' (e.g., findBy...)");
        }

        String predicate = methodName.substring(byIndex + 2);

        PartTree tree = new PartTree(predicate);

        StringBuilder jpql = new StringBuilder("SELECT e FROM ")
                .append(entityClass.getSimpleName()).append(" e WHERE ");

        int paramCounter = 1;
        var orParts = tree.getOrParts();

        for (int i = 0; i < orParts.size(); i++) {
            jpql.append("(");
            var andParts = orParts.get(i).getAndParts();

            for (int j = 0; j < andParts.size(); j++) {
                Part part = andParts.get(j);

                validateProperty(entityClass, part.getProperty());

                jpql.append("e.").append(part.getProperty())
                        .append(part.getType().getOperator());

                if (part.getType().requiresParameter()) {
                    jpql.append("?").append(paramCounter++);
                }

                if (j < andParts.size() - 1) {
                    jpql.append(" AND ");
                }
            }
            jpql.append(")");

            if (i < orParts.size() - 1) {
                jpql.append(" OR ");
            }
        }

        System.out.println("Resolved JPQL: " + jpql);
        return new QueryDescriptor(jpql.toString(), false);
    }

    private void validateProperty(Class<?> entityClass, String property) {
        try {
            boolean found = false;
            Class<?> current = entityClass;
            while (current != null) {
                try {
                    current.getDeclaredField(property);
                    found = true;
                    break;
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }
            if (!found) throw new NoSuchFieldException();
        } catch (NoSuchFieldException e) {
            throw new PropertyResolveException("Entity " + entityClass.getSimpleName() +
                    " has no property named '" + property + "'");
        }
    }
}