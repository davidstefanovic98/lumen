package io.lumen.core.component;

import io.lumen.core.exception.CircularDependencyException;
import io.lumen.core.exception.MissingDependencyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves dependencies between lights, building the dependency graph.
 * Detects circular dependencies and ensures all required dependencies exist.
 */
public class LightResolver {

    /**
     * Resolve all dependencies for a light.
     * This is called recursively to ensure dependencies are resolved depth-first.
     */
    public void resolve(LightInstance light, Map<String, LightInstance> allLights) {
        if (light.getState() != LightInstance.LightState.REGISTERED) {
            return;
        }

        light.setState(LightInstance.LightState.RESOLVING);

        for (Dependency dep : light.getMetadata().getConstructorDeps()) {
            resolveDependency(light, dep, allLights);
        }

        light.setState(LightInstance.LightState.RESOLVED);
    }

    /**
     * Resolve a single dependency.
     */
    protected void resolveDependency(LightInstance light, Dependency dep, Map<String, LightInstance> allLights) {
        if (dep.isCollection()) {
            List<LightInstance> matches = findAllMatches(dep, allLights);

            if (matches.isEmpty() && dep.isRequired()) {
                throw new MissingDependencyException(
                        "No lights found for collection dependency " + dep +
                                " required by " + light.getName()
                );
            }

            for (LightInstance match : matches) {
                checkCircularDependency(light, match);
                resolve(match, allLights);
                light.addResolvedDependency(dep, match);
            }
        } else {
            // For single dependency, find exactly one match
            LightInstance depLight = findMatch(dep, allLights);

            if (depLight == null && dep.isRequired()) {
                throw new MissingDependencyException(
                        "No light found for dependency " + dep +
                                " required by " + light.getName()
                );
            }

            if (depLight != null) {
                checkCircularDependency(light, depLight);
                resolve(depLight, allLights); // Recursive resolution
                light.addResolvedDependency(dep, depLight);
            }
        }
    }

    /**
     * Find a single light that matches the dependency.
     * Extension point for qualifier-based matching.
     */
    protected LightInstance findMatch(Dependency dep, Map<String, LightInstance> lights) {
        // Match by name first if specified
        if (dep.getName() != null) {
            LightInstance light = lights.get(dep.getName());
            if (light != null && dep.matches(light.getMetadata())) {
                return light;
            }
        }

        // Otherwise match by type
        for (LightInstance light : lights.values()) {
            if (dep.matches(light.getMetadata())) {
                return light;
            }
        }

        return null;
    }

    /**
     * Find all lights that match the dependency (for collections).
     */
    protected List<LightInstance> findAllMatches(Dependency dep, Map<String, LightInstance> lights) {
        List<LightInstance> matches = new ArrayList<>();

        for (LightInstance light : lights.values()) {
            if (dep.matches(light.getMetadata())) {
                matches.add(light);
            }
        }

        return matches;
    }

    /**
     * Check for circular dependency.
     */
    protected void checkCircularDependency(LightInstance dependent, LightInstance dependency) {
        if (dependency.getState() == LightInstance.LightState.RESOLVING) {
            throw new CircularDependencyException(
                    "Circular dependency detected: " +
                            dependent.getName() + " -> " + dependency.getName()
            );
        }
    }
}