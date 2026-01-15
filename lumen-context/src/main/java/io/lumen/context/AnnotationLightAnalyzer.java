package io.lumen.context;

import io.lumen.annotations.Lazy;
import io.lumen.annotations.Profile;
import io.lumen.annotations.Scope;
import io.lumen.annotations.Value;
import io.lumen.core.component.*;
import io.lumen.core.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AnnotationLightAnalyzer implements LightAnalyzer {

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

    protected LightMetadata analyzeFactory(LightDefinition definition) {
        Executable exec = definition.getExecutable();

        if (exec == null) {
            return new LightMetadata(definition, null, List.of());
        }

        if (exec.isAnnotationPresent(Lazy.class)) {
            definition.setLazy(true);
        }

        Scope scopeAnn = exec.getAnnotation(Scope.class);
        if (scopeAnn != null) {
            definition.setScope(scopeAnn.value());
        }

        Profile profileAnn = exec.getAnnotation(Profile.class);
        if (profileAnn != null) {
            definition.setProfile(profileAnn.value());
        }

        return new LightMetadata(
                definition,
                null,
                extractDependencies(exec.getParameters())
        );
    }


    protected List<Dependency> extractDependencies(Parameter[] parameters) {
        return Arrays.stream(parameters)
                .map(this::createDependency)
                .toList();
    }

    protected Dependency createDependency(Parameter parameter) {
        Class<?> type = parameter.getType();
        boolean isCollection = Collection.class.isAssignableFrom(type);
        if (isCollection) {
            type = ReflectionUtil.getGenericType(parameter);
        }

        Value valueAnnotation = parameter.getAnnotation(Value.class);
        if (valueAnnotation != null) {
            String key = valueAnnotation.value();
            return new Dependency(
                    type,
                    null,
                    true,
                    isCollection,
                    null,
                    Dependency.DependencyType.VALUE,
                    key
            );
        }

        return new Dependency(
                type,
                parameter.getName(),
                true,
                isCollection,
                null,
                Dependency.DependencyType.LIGHT,
                null
        );
    }

    protected LightMetadata analyzeClass(LightDefinition definition) {
        Class<?> type = definition.getType();

        boolean lazy = type.isAnnotationPresent(Lazy.class);
        definition.setLazy(lazy);

        if (type.isAnnotationPresent(Scope.class)) {
            ScopeType scope = type.getAnnotation(Scope.class).value();
            definition.setScope(scope);
        }

        if (type.isAnnotationPresent(Profile.class)) {
            String profile = type.getAnnotation(Profile.class).value();
            definition.setProfile(profile);
        }

        Constructor<?> constructor = ReflectionUtil.findConstructor(type);
        List<Dependency> constructorDeps = extractDependencies(constructor.getParameters());

        return new LightMetadata(definition, constructor, constructorDeps);
    }
}

