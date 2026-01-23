package io.lumen.aop.proxy;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class ByteBuddyConfigInterceptor {
    private final LightContainer container;

    public ByteBuddyConfigInterceptor(LightContainer container) {
        this.container = container;
    }

    @RuntimeType
    public Object intercept(@Origin Method method,
                            @SuperCall Callable<?> clientProxy) throws Exception {

        String lightName = method.getName();
        LightInstance light = container.internals().getLightInstance(lightName);

        if (light != null && light.getState() == LightInstance.LightState.READY) {
            return light.getInstance();
        }
        return clientProxy.call();
    }
}
