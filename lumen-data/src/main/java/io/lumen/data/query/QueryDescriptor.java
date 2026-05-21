package io.lumen.data.query;

public record QueryDescriptor(
        String query,
        boolean isNative,
        boolean modifying,
        QueryType queryType,
        boolean namedParams,
        String countQuery
) {
    public enum QueryType { SELECT, COUNT, EXISTS, DELETE }

    public QueryDescriptor(String query, boolean isNative) {
        this(query, isNative, false, QueryType.SELECT, false, null);
    }

    public QueryDescriptor(String query, boolean isNative, QueryType queryType) {
        this(query, isNative, false, queryType, false, null);
    }
}
