package io.lumen.core.component.injection;

import io.lumen.core.annotation.Inject;
import io.lumen.core.annotation.Value;
import io.lumen.core.component.ContainerInternalAccess;
import io.lumen.core.component.LightContainer;
import io.lumen.core.exception.NoLightFoundException;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import static io.lumen.core.util.Utils.stripPlaceholder;

final class InjectionUtils {

    private InjectionUtils() {}

    static Optional<Object> resolveOptional(
            AnnotatedElement element,
            Class<?> type,
            LightContainer container
    ) {
        boolean required = true;
        Inject inject = element.getAnnotation(Inject.class);
        if (inject != null) required = inject.required();

        try {
            if (element.isAnnotationPresent(Value.class)) {
                String key = element.getAnnotation(Value.class).value();
                Object value = container.getApplicationContext()
                        .getEnvironment()
                        .getProperty(stripPlaceholder(key), type);
                return Optional.ofNullable(value);
            }

            ContainerInternalAccess internals = container.internals();
            Object value = internals.getLightByType(type);
            return Optional.ofNullable(value);
        } catch (NoLightFoundException e) {
            if (!required) return Optional.empty();
            throw e;
        }
    }
}
