package io.lumen.core.component.injection;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.exception.LightInstantiationException;
import io.lumen.core.util.ReflectionUtil;
import io.lumen.core.util.Utils;

import java.lang.reflect.Field;
import java.util.Optional;

public class FieldInjectionStep implements InjectionStep {

    private static final String INJECT_CLASS = "io.lumen.core.annotation.Inject";
    private static final String VALUE_CLASS  = "io.lumen.core.annotation.Value";

    @Override
    public void inject(Object instance, LightInstance light, LightContainer container) {
        Class<?> type = instance.getClass();

        for (Field field : type.getDeclaredFields()) {
            if (!ReflectionUtil.hasAnnotationByName(field, INJECT_CLASS)) continue;

            field.setAccessible(true);

            // @Value on an @Inject field — resolve from environment
            String valueKey = ReflectionUtil.getAnnotationStringValue(field, VALUE_CLASS);
            if (valueKey != null) {
                String key = Utils.stripPlaceholder(valueKey);
                Object val = container.getApplicationContext().getEnvironment()
                        .getProperty(key, field.getType());
                if (val != null) {
                    try { field.set(instance, val); }
                    catch (IllegalAccessException e) {
                        throw new LightInstantiationException("Failed to inject @Value field " + field, e);
                    }
                }
                continue;
            }

            Optional<Object> valueOpt = InjectionUtils.resolveOptional(field, field.getType(), container);
            if (valueOpt.isPresent()) {
                try { field.set(instance, valueOpt.get()); }
                catch (IllegalAccessException e) {
                    throw new LightInstantiationException("Failed to inject field " + field, e);
                }
            }
        }
    }
}