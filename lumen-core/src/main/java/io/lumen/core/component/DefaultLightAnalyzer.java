package io.lumen.core.component;

import io.lumen.core.exception.LightInitializationException;
import io.lumen.core.util.ParameterNameDiscoverer;
import io.lumen.core.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.lumen.core.util.ReflectionUtil.getGenericType;

/**
 * Default implementation that uses pure reflection without annotations.
 * This is the core analyzer used when no annotation support is available.
 */
public class DefaultLightAnalyzer implements LightAnalyzer {

    @Override
    public LightMetadata analyze(LightDefinition definition) {
        if (definition.getSource() == LightDefinition.LightSource.INSTANCE) {
            return new LightMetadata(definition, null, new ArrayList<>());
        }

        if (definition.getSource() == LightDefinition.LightSource.FACTORY) {
            return analyzeFactory(definition);
        }

        return analyzeClass(definition);
    }

    protected LightMetadata analyzeClass(LightDefinition definition) {
        Class<?> type = definition.getType();

        if (type.isInterface()) {
            throw new LightInitializationException(
                    "Cannot instantiate interface: " + type.getName() +
                            ". Register a concrete implementation or use a factory."
            );
        }

        Constructor<?> constructor = ReflectionUtil.findConstructor(type);
        List<Dependency> constructorDeps = extractDependencies(constructor.getParameters());

        return new LightMetadata(definition, constructor, constructorDeps);
    }

    protected LightMetadata analyzeFactory(LightDefinition definition) {
        // Factory-based lights don't have constructor dependencies
        // The factory handles creation
        return new LightMetadata(definition, null, new ArrayList<>());
    }

    /**
     * Extract dependencies from constructor parameters.
     */
    protected List<Dependency> extractDependencies(Parameter[] parameters) {
        return Arrays.stream(parameters)
                .map(this::createDependency)
                .toList();
    }

    /**
     * Create a dependency from a parameter.
     * Default implementation uses parameter type and name.
     */
    protected Dependency createDependency(Parameter parameter) {
        Class<?> type = parameter.getType();
        String name = ParameterNameDiscoverer.getParameterName(parameter);

        boolean isCollection = Collection.class.isAssignableFrom(type);
        if (isCollection) {
            type = getGenericType(parameter);
        }

        return new Dependency(type, name, true, isCollection, null, Dependency.DependencyType.LIGHT, null, false);
    }
}