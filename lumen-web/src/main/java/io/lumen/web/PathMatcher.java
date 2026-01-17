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

    private PathMatcher(Pattern pattern, List<String> variableNames) {
        this.pattern = pattern;
        this.variableNames = variableNames;
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
        return new PathMatcher(compiledPattern, variables);
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
}