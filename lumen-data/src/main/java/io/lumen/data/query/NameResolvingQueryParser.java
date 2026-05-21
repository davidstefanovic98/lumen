package io.lumen.data.query;

import io.lumen.data.exception.PropertyResolveException;
import io.lumen.data.query.QueryDescriptor.QueryType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class NameResolvingQueryParser implements QueryParser {

    private static final String[] SELECT_PREFIXES = {"find", "get", "read", "query", "search"};
    private static final String[] COUNT_PREFIXES  = {"count"};
    private static final String[] EXISTS_PREFIXES = {"exists"};
    private static final String[] DELETE_PREFIXES = {"delete", "remove"};

    @Override
    public QueryDescriptor parse(Method method, Class<?> entityClass) {
        String name = method.getName();

        int byIndex = name.indexOf("By");
        if (byIndex == -1) {
            throw new IllegalArgumentException(
                    "Derived method '" + name + "' must contain 'By' (e.g., findByName)");
        }

        String prefix    = name.substring(0, byIndex).toLowerCase();
        QueryType type   = resolveQueryType(prefix);
        String predicate = name.substring(byIndex + 2);

        // Extract optional OrderBy suffix
        String orderBy = "";
        int orderByIdx = predicate.indexOf("OrderBy");
        if (orderByIdx != -1) {
            orderBy  = buildOrderByClause(predicate.substring(orderByIdx + 7));
            predicate = predicate.substring(0, orderByIdx);
        }

        String entity = entityClass.getSimpleName();
        String selectClause = switch (type) {
            case COUNT, EXISTS -> "SELECT COUNT(e) FROM " + entity + " e";
            case DELETE        -> "DELETE FROM " + entity + " e";
            default            -> "SELECT e FROM " + entity + " e";
        };

        String whereClause = buildWhereClause(predicate, entityClass);
        String jpql = selectClause + " WHERE " + whereClause + orderBy;

        return new QueryDescriptor(jpql, false, type);
    }

    // -------------------------------------------------------------------------
    // WHERE clause
    // -------------------------------------------------------------------------

    private String buildWhereClause(String predicate, Class<?> entityClass) {
        PartTree tree = new PartTree(predicate);
        StringBuilder where = new StringBuilder();
        int[] counter = {1};

        List<PartTree.OrPart> orParts = tree.getOrParts();
        for (int i = 0; i < orParts.size(); i++) {
            where.append("(");
            List<Part> andParts = orParts.get(i).getAndParts();
            for (int j = 0; j < andParts.size(); j++) {
                Part part = andParts.get(j);
                validatePropertyPath(entityClass, part.getProperty());
                appendCondition(where, part, counter);
                if (j < andParts.size() - 1) where.append(" AND ");
            }
            where.append(")");
            if (i < orParts.size() - 1) where.append(" OR ");
        }
        return where.toString();
    }

    private void appendCondition(StringBuilder jpql, Part part, int[] counter) {
        String prop = "e." + part.getProperty();
        switch (part.getType()) {
            case BETWEEN ->
                jpql.append(prop)
                    .append(" BETWEEN ?").append(counter[0]++)
                    .append(" AND ?").append(counter[0]++);
            case IN ->
                jpql.append(prop).append(" IN (?").append(counter[0]++).append(")");
            case CONTAINING ->
                jpql.append(prop)
                    .append(" LIKE CONCAT('%', ?").append(counter[0]++).append(", '%')");
            case STARTING_WITH ->
                jpql.append(prop)
                    .append(" LIKE CONCAT(?").append(counter[0]++).append(", '%')");
            case ENDING_WITH ->
                jpql.append(prop)
                    .append(" LIKE CONCAT('%', ?").append(counter[0]++).append(")");
            case IGNORE_CASE ->
                jpql.append("LOWER(").append(prop)
                    .append(") = LOWER(?").append(counter[0]++).append(")");
            case CONTAINING_IGNORE_CASE ->
                jpql.append("LOWER(").append(prop)
                    .append(") LIKE LOWER(CONCAT('%', ?").append(counter[0]++).append(", '%'))");
            case STARTING_WITH_IGNORE_CASE ->
                jpql.append("LOWER(").append(prop)
                    .append(") LIKE LOWER(CONCAT(?").append(counter[0]++).append(", '%'))");
            case ENDING_WITH_IGNORE_CASE ->
                jpql.append("LOWER(").append(prop)
                    .append(") LIKE LOWER(CONCAT('%', ?").append(counter[0]++).append("))");
            default -> {
                jpql.append(prop).append(part.getType().getOperator());
                if (part.getType().requiresParameter()) {
                    jpql.append("?").append(counter[0]++);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ORDER BY suffix
    // -------------------------------------------------------------------------

    private String buildOrderByClause(String orderBySource) {
        // e.g. "NameAscAgeDesc" → " ORDER BY e.name ASC, e.age DESC"
        String[] segments = orderBySource.split("(?<=Asc|Desc)(?=[A-Z])");
        List<String> tokens = new ArrayList<>();
        for (String seg : segments) {
            String dir  = "ASC";
            String prop = seg;
            if (seg.endsWith("Desc")) {
                dir  = "DESC";
                prop = seg.substring(0, seg.length() - 4);
            } else if (seg.endsWith("Asc")) {
                prop = seg.substring(0, seg.length() - 3);
            }
            if (prop.isEmpty()) continue;
            String field = Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
            tokens.add("e." + field + " " + dir);
        }
        return tokens.isEmpty() ? "" : " ORDER BY " + String.join(", ", tokens);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private QueryType resolveQueryType(String prefix) {
        for (String p : COUNT_PREFIXES)  if (prefix.startsWith(p)) return QueryType.COUNT;
        for (String p : EXISTS_PREFIXES) if (prefix.startsWith(p)) return QueryType.EXISTS;
        for (String p : DELETE_PREFIXES) if (prefix.startsWith(p)) return QueryType.DELETE;
        return QueryType.SELECT;
    }

    private void validatePropertyPath(Class<?> entityClass, String propertyPath) {
        Class<?> current = entityClass;
        for (String segment : propertyPath.split("\\.")) {
            current = resolveField(current, segment);
            if (current == null) {
                throw new PropertyResolveException(
                        "Entity " + entityClass.getSimpleName() +
                        " has no property path '" + propertyPath + "'");
            }
        }
    }

    private Class<?> resolveField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                return f.getType();
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
