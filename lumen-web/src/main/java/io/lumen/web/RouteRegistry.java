package io.lumen.web;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.util.ParameterNameDiscoverer;
import io.lumen.core.util.ReflectionUtil;
import io.lumen.web.annotation.PathVariable;
import io.lumen.web.exception.AmbiguousMappingException;
import io.lumen.web.exception.PathVariableNotFoundException;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Registry for storing and managing route mappings.
 */
public class RouteRegistry {
    private static final Logger logger = LoggerFactory.getLogger(RouteRegistry.class);
    private final List<RouteEntry> routes = new ArrayList<>();

    public void register(Route route) {
        PathMatcher newMatcher = PathMatcher.compile(route.getPathPattern());
        validatePathVariables(route, newMatcher);
        for (RouteEntry existing : routes) {
            if (existing.route.getHttpMethod().equalsIgnoreCase(route.getHttpMethod()) &&
                    existing.matcher.isAmbiguous(newMatcher)) {
                throw new AmbiguousMappingException(
                        "Ambiguous mapping: [" + route.getHttpMethod() + " " + route.getPathPattern() + "] in controller "
                                + controllerSimpleName(route) + "#" + route.getMethod().getName() +
                                " conflicts with existing [" + existing.route.getHttpMethod() + " " + existing.route.getPathPattern() + "] in controller "
                                + controllerSimpleName(existing.route) + "#" + existing.route.getMethod().getName()
                );
            }
//            if (newMatcher.couldShadowLiteral(existing.matcher)) {
//                logger.debug(String.format(
//                        "Route [%s %s] in controller %s#%s may shadow existing route [%s %s] in controller %s#%s%n",
//                        route.getHttpMethod(),
//                        route.getPathPattern(),
//                        route.getController().getClass().getSimpleName(),
//                        route.getMethod().getName(),
//                        existing.route.getHttpMethod(),
//                        existing.route.getPathPattern(),
//                        existing.route.getController().getClass().getSimpleName(),
//                        existing.route.getMethod().getName()
//                ));
//            }
        }
        routes.add(new RouteEntry(route, newMatcher));
        // Sort routes by specificity (more specific routes first), to ensure correct matching order
        // this is necessary because JVM does not guarantee order
        routes.sort((r1, r2) -> Integer.compare(r2.specificity(), r1.specificity()));
    }

    /**
     * A route's declared {@code {name}} segments are fixed at registration time - unlike a
     * missing/malformed request parameter, a {@code @PathVariable} that doesn't match any
     * segment in its own route's pattern will fail on every single request to that route,
     * regardless of what the client sends. That's a static defect in the controller method
     * itself, so it's caught here at startup instead of on the route's first request.
     */
    private void validatePathVariables(Route route, PathMatcher matcher) {
        List<String> declaredVariables = matcher.getVariableNames();
        for (Parameter parameter : route.getMethod().getParameters()) {
            if (!parameter.isAnnotationPresent(PathVariable.class)) continue;

            PathVariable annotation = parameter.getAnnotation(PathVariable.class);
            String name = annotation.value().isEmpty()
                    ? ParameterNameDiscoverer.getParameterName(parameter)
                    : annotation.value();

            if (!declaredVariables.contains(name)) {
                throw new PathVariableNotFoundException(String.format(
                        "@PathVariable '%s' on %s#%s has no matching '{%s}' segment in route pattern [%s %s]. " +
                                "Check for a typo in the @PathVariable name or the route pattern.",
                        name, controllerSimpleName(route), route.getMethod().getName(),
                        name, route.getHttpMethod(), route.getPathPattern()
                ));
            }
        }
    }

    /**
     * Controllers wrapped by a post-processor (e.g. @Transactional, @PreAuthorize) are
     * ByteBuddy proxy instances by the time routes are scanned, so route.getController().getClass()
     * would show a generated name like "TaskController$ByteBuddy$EV0DpoOT" in error messages -
     * confusing, since it's not a class anyone wrote. This resolves to the real controller class.
     */
    private static String controllerSimpleName(Route route) {
        return ReflectionUtil.getUserClass(route.getController().getClass()).getSimpleName();
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