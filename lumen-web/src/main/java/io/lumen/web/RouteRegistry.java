package io.lumen.web;

import io.lumen.context.annotation.Component;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.exception.AmbiguousMappingException;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry for storing and managing route mappings.
 */
@Component
public class RouteRegistry {
    private static final Logger logger = LoggerFactory.getLogger(RouteRegistry.class);
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
            if (newMatcher.couldShadowLiteral(existing.matcher)) {
                logger.warn(String.format(
                        "WARNING: Route [%s %s] in controller %s#%s may shadow existing route [%s %s] in controller %s#%s%n",
                        route.getHttpMethod(),
                        route.getPathPattern(),
                        route.getController().getClass().getSimpleName(),
                        route.getMethod().getName(),
                        existing.route.getHttpMethod(),
                        existing.route.getPathPattern(),
                        existing.route.getController().getClass().getSimpleName(),
                        existing.route.getMethod().getName()
                ));
            }
        }
        routes.add(new RouteEntry(route, newMatcher));
        // Sort routes by specificity (more specific routes first), to ensure correct matching order
        // this is necessary because JVM does not guarantee order
        routes.sort((r1, r2) -> Integer.compare(r2.specificity(), r1.specificity()));
    }

    RouteMatch findMatch(String path, String httpMethod) {
        for (RouteEntry entry : routes) {
            if (entry.matches(path, httpMethod)) {
                return entry.createMatch(path);
            }
        }
        return null;
    }

    public int getRouteCount() {
        return routes.size();
    }

    public List<Route> getAllRoutes() {
        List<Route> allRoutes = new ArrayList<>();
        for (RouteEntry entry : routes) {
            allRoutes.add(entry.route);
        }
        return allRoutes;
    }

    private record RouteEntry(Route route, PathMatcher matcher) {
        int specificity() {
            return matcher.getSpecificity();
        }

        boolean matches(String path, String httpMethod) {
            return route.getHttpMethod().equalsIgnoreCase(httpMethod) && matcher.matches(path);
        }

        RouteMatch createMatch(String path) {
            return new RouteMatch(route, matcher.extractVariables(path));
        }
    }
}