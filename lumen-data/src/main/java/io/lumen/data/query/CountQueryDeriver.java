package io.lumen.data.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives a {@code SELECT COUNT(alias) FROM ...} query from an arbitrary SELECT JPQL string, for
 * {@code Page}-returning repository methods that don't declare an explicit
 * {@code @Query(countQuery = ...)}.
 */
public final class CountQueryDeriver {

    private static final Pattern FROM_ALIAS = Pattern.compile("(?i)FROM\\s+\\w+\\s+(\\w+)");

    private CountQueryDeriver() {}

    public static String derive(String jpql) {
        String withoutOrderBy = jpql.replaceAll("(?i)\\s+ORDER\\s+BY.+$", "");
        String fromClauseOnward = withoutOrderBy.substring(findTopLevelFromIndex(withoutOrderBy));
        Matcher m = FROM_ALIAS.matcher(fromClauseOnward);
        String alias = m.find() ? m.group(1) : "e";
        return "SELECT COUNT(" + alias + ") " + fromClauseOnward;
    }

    /**
     * Finds the index of the query's own top-level FROM keyword, ignoring any FROM inside a
     * parenthesized subquery (e.g. a subquery in the SELECT projection list) or a string literal.
     * A regex alone can't express "not inside parentheses", which is why the previous
     * implementation's reluctant match stopped at the first FROM it saw, subquery or not.
     */
    private static int findTopLevelFromIndex(String jpql) {
        int depth = 0;
        boolean inString = false;
        char quoteChar = 0;
        for (int i = 0; i < jpql.length(); i++) {
            char c = jpql.charAt(i);
            if (inString) {
                if (c == quoteChar) inString = false;
                continue;
            }
            switch (c) {
                case '\'', '"' -> { inString = true; quoteChar = c; }
                case '(' -> depth++;
                case ')' -> depth--;
                default -> {
                    if (depth == 0 && isFromKeywordAt(jpql, i)) return i;
                }
            }
        }
        throw new IllegalArgumentException("Could not find a top-level FROM clause to derive a count query from: " + jpql);
    }

    private static boolean isFromKeywordAt(String s, int i) {
        if (!s.regionMatches(true, i, "FROM", 0, 4)) return false;
        boolean leftBoundary = i == 0 || !Character.isJavaIdentifierPart(s.charAt(i - 1));
        int after = i + 4;
        boolean rightBoundary = after == s.length() || !Character.isJavaIdentifierPart(s.charAt(after));
        return leftBoundary && rightBoundary;
    }
}