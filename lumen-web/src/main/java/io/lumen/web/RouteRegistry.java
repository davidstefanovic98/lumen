package io.lumen.web;

import io.lumen.web.exception.AmbiguousMappingException;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry for storing and managing route mappings.
 */
public class RouteRegistry {

    private final List<RouteEntry> routes = new ArrayList<>();

    public void register(Route route) {
        PathMatcher newMatcher = PathMatcher.compile(route.getPathPattern());
        for (RouteEntry existing : routes) {
            if (existing.route.getHttpMethod().equalsIgnoreCase(route.getHttpMethod()) &&
                    existing.matcher.isAmbiguous(newMatcher)) {
                throw new AmbiguousMappingException(
                        "Ambiguous mapping: [" + route.getHttpMethod() + " " + route.getPathPattern() + "] in controller "
                                + route.getController().getClass().getSimpleName() + "#" + route.getMethod().getName() +
                                " conflicts with existing [" + existing.route.getHttpMethod() + " " + existing.route.getPathPattern() + "] in controller "
                                + existing.route.getController().getClass().getSimpleName() + "#" + existing.route.getMethod().getName()
                );
            }
        }
        routes.add(new RouteEntry(route, newMatcher));
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
            return route.getHttpMethod().equalsIgnoreCase(httpMethod) && matcher.matches(path);
        }

        RouteMatch createMatch(String path) {
            return new RouteMatch(route, matcher.extractVariables(path));
        }
    }
}