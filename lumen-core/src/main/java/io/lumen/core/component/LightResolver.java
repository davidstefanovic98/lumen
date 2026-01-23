package io.lumen.core.component;

import io.lumen.core.exception.CircularDependencyException;
import io.lumen.core.exception.MissingDependencyException;

import java.util.*;

/**
 * Resolves dependencies between lights, building the dependency graph.
 * Detects circular dependencies and ensures all required dependencies exist.
 */
public class LightResolver {

    /**
     * Entry point to resolve a light and its dependencies.
     * Tracks resolution path to detect circular dependencies.
     */
    public void resolve(LightInstance light, Map<String, LightInstance> allLights) {
        Deque<LightInstance> path = new ArrayDeque<>();
        resolve(light, allLights, path);
    }

    /**
     * Recursive resolution with path tracking.
     */
    private void resolve(LightInstance light, Map<String, LightInstance> allLights, Deque<LightInstance> path) {
        if (light.getState() == LightInstance.LightState.RESOLVED || light.getState() == LightInstance.LightState.READY) {
            return;
        }

        if (path.contains(light)) {
            String cycleMessage = buildCircularPathMessage(path, light);
            throw new CircularDependencyException(cycleMessage);
        }

        path.push(light);
        light.setState(LightInstance.LightState.RESOLVING);

        for (Dependency dep : light.getMetadata().getConstructorDeps()) {
            resolveDependency(light, dep, allLights, path);
        }

        light.setState(LightInstance.LightState.RESOLVED);
        path.pop();
    }

    /**
     * Resolve a single dependency with path tracking.
     */
    private void resolveDependency(LightInstance light, Dependency dep, Map<String, LightInstance> allLights, Deque<LightInstance> path) {
        if (dep.getDepType() != Dependency.DependencyType.LIGHT) {
            return;
        }

        if (dep.isCollection()) {
            List<LightInstance> matches = findAllMatches(dep, allLights);

            if (matches.isEmpty() && dep.isRequired()) {
                throw new MissingDependencyException(buildMissingMessage(light, dep));
            }

            for (LightInstance match : matches) {
                resolve(match, allLights, path);
                light.addResolvedDependency(dep, match);
            }
        } else {
            LightInstance depLight = findMatch(dep, allLights);

            if (depLight == null && dep.isRequired() && !dep.isLazy()) {
                throw new MissingDependencyException(buildMissingMessage(light, dep));
            }

            if (depLight != null) {
                resolve(depLight, allLights, path);
                light.addResolvedDependency(dep, depLight);
            }
        }
    }

    private String buildMissingMessage(LightInstance light, Dependency dep) {
        return String.format(
                "Missing dependency for light '%s' [%s]: required dependency '%s' of type %s not found",
                light.getName(),
                light.getType().getSimpleName(),
                dep.getName() != null ? dep.getName() : dep.getType().getSimpleName(),
                dep.getType().getSimpleName()
        );
    }

    protected LightInstance findMatch(Dependency dep, Map<String, LightInstance> lights) {
        if (dep.getName() != null) {
            LightInstance light = lights.get(dep.getName());
            if (light != null && dep.matches(light.getMetadata())) {
                return light;
            }
        }

        for (LightInstance light : lights.values()) {
            if (dep.matches(light.getMetadata())) {
                return light;
            }
        }
        return null;
    }

    protected List<LightInstance> findAllMatches(Dependency dep, Map<String, LightInstance> lights) {
        List<LightInstance> matches = new ArrayList<>();
        for (LightInstance light : lights.values()) {
            if (dep.matches(light.getMetadata())) {
                matches.add(light);
            }
        }
        return matches;
    }

    private String buildCircularPathMessage(Deque<LightInstance> path, LightInstance repeatingLight) {
        StringBuilder sb = new StringBuilder("Circular dependency detected: ");
        Iterator<LightInstance> iterator = path.descendingIterator();
        boolean found = false;
        while (iterator.hasNext()) {
            LightInstance light = iterator.next();
            if (light == repeatingLight) {
                found = true;
            }
            if (found) {
                sb.append(light.getName()).append(" -> ");
            }
        }
        sb.append(repeatingLight.getName());
        return sb.toString();
    }
}
