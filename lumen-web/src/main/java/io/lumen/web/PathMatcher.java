package io.lumen.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple path matcher that supports path variables in the form of {variableName}.
 */
class PathMatcher {
    private final Pattern pattern;
    private final List<String> variableNames;
    private final String patternString;
    private final int specificity;

    private PathMatcher(Pattern pattern, List<String> variableNames, String patternString) {
        this.pattern = pattern;
        this.variableNames = variableNames;
        this.patternString = patternString;
        this.specificity = computeSpecificity(patternString);
    }

    static PathMatcher compile(String pathPattern) {
        List<String> variables = new ArrayList<>();
        Pattern varPattern = Pattern.compile("\\{([^/}]+)}");
        Matcher matcher = varPattern.matcher(pathPattern);

        StringBuilder regex = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            variables.add(varName);
            matcher.appendReplacement(regex, "(?<" + varName + ">[^/]+)");
        }
        matcher.appendTail(regex);

        Pattern compiledPattern = Pattern.compile("^" + regex + "$");
        return new PathMatcher(compiledPattern, variables, pathPattern);
    }

    boolean matches(String path) {
        return pattern.matcher(path).matches();
    }

    Map<String, String> extractVariables(String path) {
        Map<String, String> variables = new HashMap<>();
        Matcher matcher = pattern.matcher(path);

        if (matcher.matches()) {
            for (String varName : variableNames) {
                variables.put(varName, matcher.group(varName));
            }
        }

        return variables;
    }

    /**
     * Checks if this path pattern overlaps with another path pattern.
     * Two patterns overlap if there exists at least one path that matches both patterns.
     * Checks literally segment by segment, considering wildcards and variables.
     *
     * @param other the other PathMatcher to compare with
     * @return true if the patterns overlap, false otherwise
     */
    boolean isAmbiguous(PathMatcher other) {
        String[] seg1 = this.patternString.split("/");
        String[] seg2 = other.patternString.split("/");

        // Check for ** wildcard (matches everything after)
        for (int i = 0; i < seg1.length; i++) {
            if (seg1[i].equals("**")) {
                // ** at position i means this pattern matches anything from here on
                // It overlaps with other if other has at least i segments
                return seg2.length >= i;
            }
        }
        for (int i = 0; i < seg2.length; i++) {
            if (seg2[i].equals("**")) {
                return seg1.length >= i;
            }
        }

        // No ** wildcards, so lengths must match
        if (seg1.length != seg2.length) {
            return false;
        }

        for (int i = 0; i < seg1.length; i++) {
            String s1 = seg1[i];
            String s2 = seg2[i];

            boolean s1Var = (s1.startsWith("{") && s1.endsWith("}")) || s1.equals("*");
            boolean s2Var = (s2.startsWith("{") && s2.endsWith("}")) || s2.equals("*");

            // Both are literals
            if (!s1Var && !s2Var) {
                if (!s1.equals(s2)) {
                    return false; // Different literals = no overlap
                }
                continue;
            }

            // One is literal, one is variable/wildcard = NOT ambiguous (literal is more specific)
            if (s1Var != s2Var) {
                return false;
            }

            // Both are variables/wildcards = continue checking
            // Note: {id} and * are treated the same (both match any single segment)
        }

        return true;
    }

    int getSpecificity() {
        return specificity;
    }

    boolean couldShadowLiteral(PathMatcher other) {
        String[] seg1 = this.patternString.split("/");
        String[] seg2 = other.patternString.split("/");

        if (seg1.length != seg2.length) {
            return false;
        }

        for (int i = 0; i < seg1.length; i++) {
            boolean s1Var = (seg1[i].startsWith("{") && seg1[i].endsWith("}")) || seg1[i].equals("*");
            boolean s2Var = (seg2[i].startsWith("{") && seg2[i].endsWith("}")) || seg2[i].equals("*");

            if (s1Var && !s2Var) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate specificity score for this path pattern.
     * Higher score means more specific.
     * Literal segments score 2, variable segments score 1, wildcard (**) scores 0.
     *
     * @return specificity score
     */
    private static int computeSpecificity(String pathPattern) {
        String[] segments = pathPattern.split("/");
        int score = 0;
        for (String s : segments) {
            if (s.equals("**")) score += 0;           // wildcard
            else if (s.startsWith("{") && s.endsWith("}")) score += 1; // variable
            else if (!s.isEmpty()) score += 2;       // literal
        }
        return score;
    }
}