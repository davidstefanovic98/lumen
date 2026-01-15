package io.lumen.context;

import io.lumen.context.annotations.*;
import io.lumen.core.component.Dependency;
import io.lumen.core.component.LightAnalyzer;
import io.lumen.core.component.LightDefinition;
import io.lumen.core.component.LightMetadata;
import io.lumen.core.conditional.Condition;
import io.lumen.core.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Analyzes LightDefinitions and produces LightMetadata, including:
 * - Dependencies
 * - Lazy / Scope / Profile
 * - Conditional classes
 * - ConditionalOnProperty / ConditionalOnClass
 */
public class AnnotationLightAnalyzer implements LightAnalyzer {

    @Override
    public LightMetadata analyze(LightDefinition definition) {
        switch (definition.getSource()) {
            case INSTANCE -> {
                return new LightMetadata(definition, null, new ArrayList<>());
            }
            case FACTORY -> {
                return analyzeFactory(definition);
            }
            case CLASS -> {
                return analyzeClass(definition);
            }
        }
        // shouldn't happen
        return null;
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

        Conditional conditionalAnn = exec.getAnnotation(Conditional.class);

        LightMetadata metadata = new LightMetadata(definition, null, extractDependencies(exec.getParameters()));

        if (conditionalAnn != null) {
            metadata.setConditionalClasses(conditionalAnn.value());
        }

        if (exec.isAnnotationPresent(ConditionalOnProperty.class)) {
            ConditionalOnProperty ann = exec.getAnnotation(ConditionalOnProperty.class);
            Condition c = new ConditionalOnPropertyCondition(ann.name(), ann.havingValue(), ann.matchIfMissing());
            metadata.addCondition(c);
        }

        if (exec.isAnnotationPresent(ConditionalOnClass.class)) {
            ConditionalOnClass ann = exec.getAnnotation(ConditionalOnClass.class);
            Condition c = new ConditionalOnClassCondition(ann.value(), ann.name());
            metadata.addCondition(c);
        }

        if (exec.isAnnotationPresent(ConditionalOnLight.class)) {
            ConditionalOnLight ann = exec.getAnnotation(ConditionalOnLight.class);
            Condition c = new ConditionalOnLightCondition(ann.value(), ann.name());
            metadata.addCondition(c);
        }

        return metadata;
    }

    protected LightMetadata analyzeClass(LightDefinition definition) {
        Class<?> type = definition.getType();

        boolean lazy = type.isAnnotationPresent(Lazy.class);
        definition.setLazy(lazy);

        if (type.isAnnotationPresent(Scope.class)) {
            definition.setScope(type.getAnnotation(Scope.class).value());
        }

        if (type.isAnnotationPresent(Profile.class)) {
            definition.setProfile(type.getAnnotation(Profile.class).value());
        }

        Constructor<?> constructor = ReflectionUtil.findConstructor(type);
        List<Dependency> deps = extractDependencies(constructor.getParameters());

        LightMetadata metadata = new LightMetadata(definition, constructor, deps);

        if (type.isAnnotationPresent(Conditional.class)) {
            Conditional conditionalAnn = type.getAnnotation(Conditional.class);
            metadata.setConditionalClasses(conditionalAnn.value());
        }

        if (type.isAnnotationPresent(ConditionalOnProperty.class)) {
            ConditionalOnProperty ann = type.getAnnotation(ConditionalOnProperty.class);
            Condition c = new ConditionalOnPropertyCondition(ann.name(), ann.havingValue(), ann.matchIfMissing());
            metadata.addCondition(c);
        }

        if (type.isAnnotationPresent(ConditionalOnClass.class)) {
            ConditionalOnClass ann = type.getAnnotation(ConditionalOnClass.class);
            Condition c = new ConditionalOnClassCondition(ann.value(), ann.name());
            metadata.addCondition(c);
        }

        if (type.isAnnotationPresent(ConditionalOnLight.class)) {
            ConditionalOnLight ann = type.getAnnotation(ConditionalOnLight.class);
            Condition c = new ConditionalOnLightCondition(ann.value(), ann.name());
            metadata.addCondition(c);
        }

        return metadata;
    }

    protected List<Dependency> extractDependencies(Parameter[] parameters) {
        return Arrays.stream(parameters).map(this::createDependency).toList();
    }

    protected Dependency createDependency(Parameter parameter) {
        Class<?> type = parameter.getType();
        boolean isCollection = Collection.class.isAssignableFrom(type);
        if (isCollection) type = ReflectionUtil.getGenericType(parameter);

        if (parameter.isAnnotationPresent(Value.class)) {
            String key = parameter.getAnnotation(Value.class).value();
            return new Dependency(type, null, true, isCollection, null, Dependency.DependencyType.VALUE, key);
        }

        return new Dependency(type, parameter.getName(), true, isCollection, null, Dependency.DependencyType.LIGHT, null);
    }
}
