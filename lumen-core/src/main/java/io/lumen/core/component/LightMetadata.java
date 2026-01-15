package io.lumen.core.component;

import io.lumen.core.conditional.Condition;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Analyzed metadata about how to create a light.
 * Contains constructor, dependencies, and other computed information.
 */
public class LightMetadata {
    private final LightDefinition definition;
    private final Constructor<?> constructor;
    private final List<Dependency> constructorDeps;
    private final List<Dependency> fieldDeps;
    private List<Condition> conditions;

    private Class<? extends Condition>[] conditionalClasses;

    public LightMetadata(LightDefinition definition,
                         Constructor<?> constructor,
                         List<Dependency> constructorDeps,
                         List<Dependency> fieldDeps) {
        this.definition = definition;
        this.constructor = constructor;
        this.constructorDeps = constructorDeps != null ? constructorDeps : Collections.emptyList();
        this.fieldDeps = fieldDeps != null ? fieldDeps : Collections.emptyList();
    }

    public LightMetadata(LightDefinition definition,
                         Constructor<?> constructor,
                         List<Dependency> constructorDeps) {
        this(definition, constructor, constructorDeps, Collections.emptyList());
    }

    public LightDefinition getDefinition() {
        return definition;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public List<Dependency> getConstructorDeps() {
        return constructorDeps;
    }

    public List<Dependency> getFieldDeps() {
        return fieldDeps;
    }

    public List<Dependency> getAllDependencies() {
        if (fieldDeps.isEmpty()) {
            return constructorDeps;
        }

        List<Dependency> all = new ArrayList<>(constructorDeps);
        all.addAll(fieldDeps);
        return all;
    }

    public Class<? extends Condition>[] getConditionalClasses() {
        return conditionalClasses;
    }

    public void setConditionalClasses(Class<? extends Condition>[] conditionalClasses) {
        this.conditionalClasses = conditionalClasses;
    }

    public void addCondition(Condition condition) {
        if (conditions == null) {
            conditions = new ArrayList<>();
        }
        conditions.add(condition);
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    @Override
    public String toString() {
        return "LightMetadata{" +
                "definition=" + definition +
                ", constructorDeps=" + constructorDeps.size() +
                ", fieldDeps=" + fieldDeps.size() +
                '}';
    }
}