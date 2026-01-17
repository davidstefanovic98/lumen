package io.lumen.core.component.injection;

import io.lumen.core.annotation.Inject;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.exception.LightInstantiationException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

public class SetterInjectionStep implements InjectionStep {

    @Override
    public void inject(
            Object instance,
            LightInstance light,
            LightContainer container
    ) {
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Inject.class))
                continue;

            if (method.getParameterCount() != 1) {
                throw new LightInstantiationException(
                        "@Inject setter must have one parameter: " + method
                );
            }

            method.setAccessible(true);
            Parameter param = method.getParameters()[0];
            Optional<Object> valueOpt = InjectionUtils.resolveOptional(method, param.getType(), container);
            if (valueOpt.isPresent()) {
                try {
                    method.invoke(instance, valueOpt.get());
                } catch (Exception e) {
                    throw new LightInstantiationException("Failed to inject setter " + method, e);
                }
            }
        }
    }
}

