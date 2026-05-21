package io.lumen.aop.proxy;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ByteBuddyLazyInterceptor {
    private final LightContainer container;
    private final LightInstance light;
    private Object delegate;

    public ByteBuddyLazyInterceptor(LightContainer container, LightInstance light) {
        this.container = container;
        this.light = light;
    }

    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args) throws Throwable {
        if (delegate == null) {
            if (light == null) {
                throw new IllegalStateException("Lazy dependency could not be resolved: LightInstance is null");
            }

            delegate = container.internals().getLightByName(light.getName());

            if (delegate == null) {
                throw new IllegalStateException("Lazy dependency '" + light.getName() + "' not found in container.");
            }
        }
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
