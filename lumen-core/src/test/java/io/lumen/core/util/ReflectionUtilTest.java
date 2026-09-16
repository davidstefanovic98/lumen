package io.lumen.core.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtilTest {

    static class Base {
        public void baseMethod() {}
        public void overridden() {}
    }

    static class Child extends Base {
        public void childMethod() {}

        @Override
        public void overridden() {}
    }

    @Test
    void getAllMethods_includesInheritedMethods() {
        List<Method> methods = ReflectionUtil.getAllMethods(Child.class);
        Set<String> names = methods.stream().map(Method::getName).collect(java.util.stream.Collectors.toSet());

        assertTrue(names.contains("baseMethod"), "inherited method should be included");
        assertTrue(names.contains("childMethod"), "declared method should be included");
    }

    @Test
    void getAllMethods_overriddenMethod_appearsOnce_asSubclassVersion() {
        List<Method> methods = ReflectionUtil.getAllMethods(Child.class);
        List<Method> overridden = methods.stream().filter(m -> m.getName().equals("overridden")).toList();

        assertEquals(1, overridden.size(), "an overridden method must not appear twice");
        assertEquals(Child.class, overridden.get(0).getDeclaringClass(),
                "the most-derived declaration should win");
    }

    @Test
    void getAllMethods_excludesObjectMethods() {
        List<Method> methods = ReflectionUtil.getAllMethods(Child.class);

        assertTrue(methods.stream().noneMatch(m -> m.getDeclaringClass() == Object.class));
    }
}