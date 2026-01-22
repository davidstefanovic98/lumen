package io.lumen.core.component.processor;

import io.lumen.core.annotation.PostConstruct;
import io.lumen.core.component.LightInstance;

import java.lang.reflect.Method;

public class PostConstructProcessor implements LightProcessor {

    public Object afterInstantiation(LightInstance light, Object instance) {
        Class<?> clazz = instance.getClass();

        if (clazz.getName().contains("$ByteBuddy")) {
            clazz = clazz.getSuperclass();
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                try {
                    method.setAccessible(true);
                    method.invoke(instance);
                } catch (Exception e) {
                    throw new RuntimeException("PostConstruct failed", e);
                }
            }
        }
        return instance;
    }
}
