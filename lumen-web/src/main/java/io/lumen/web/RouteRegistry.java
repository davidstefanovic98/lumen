package io.lumen.web;

import java.util.ArrayList;
import java.util.List;

/**
 * Clean registry for route storage and lookup
 */
public class RouteRegistry {

    private final List<RouteEntry> routes = new ArrayList<>();

    public void register(Route route) {
        routes.add(new RouteEntry(route, PathMatcher.compile(route.pathPattern())));
    }

    RouteMatch findMatch(String path, String httpMethod) {
        for (RouteEntry entry : routes) {
            if (entry.matches(path, httpMethod)) {
                return entry.createMatch(path);
            }
        }
        return null;
    }

    private record RouteEntry(Route route, PathMatcher matcher) {

        boolean matches(String path, String httpMethod) {
            return route.httpMethod().equalsIgnoreCase(httpMethod) && matcher.matches(path);
        }

        RouteMatch createMatch(String path) {
            return new RouteMatch(route, matcher.extractVariables(path));
        }
    }
}