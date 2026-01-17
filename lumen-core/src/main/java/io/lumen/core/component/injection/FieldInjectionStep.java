package io.lumen.core.component.injection;

import io.lumen.core.annotation.Inject;
import io.lumen.core.annotation.Value;
import io.lumen.core.component.ContainerInternalAccess;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.exception.LightInstantiationException;

import java.lang.reflect.Field;
import java.util.Optional;


public class FieldInjectionStep implements InjectionStep {

    @Override
    public void inject(Object instance, LightInstance light, LightContainer container) {
        Class<?> type = instance.getClass();

        for (Field field : type.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Inject.class))
                continue;

            field.setAccessible(true);

            Optional<Object> valueOpt = InjectionUtils.resolveOptional(field, field.getType(), container);

            if (valueOpt.isPresent()) {
                try {
                    field.set(instance, valueOpt.get());
                } catch (IllegalAccessException e) {
                    throw new LightInstantiationException("Failed to inject field " + field, e);
                }
            }
        }
    }
}
