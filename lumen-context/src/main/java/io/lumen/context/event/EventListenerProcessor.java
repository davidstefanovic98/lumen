package io.lumen.context.event;

import io.lumen.context.annotation.EventListener;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;

import java.lang.reflect.Method;

public class EventListenerProcessor implements LightProcessor {

    private final ApplicationEventMulticaster multicaster;

    public EventListenerProcessor(ApplicationEventMulticaster multicaster) {
        this.multicaster = multicaster;
    }

    @Override
    public Object afterInstantiation(LightInstance light, Object instance) {
        Class<?> type = instance.getClass();
        while (type != null && type != Object.class) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(EventListener.class)) {
                    method.setAccessible(true);
                    multicaster.registerListener(instance, method);
                }
            }
            type = type.getSuperclass();
        }
        return instance;
    }
}