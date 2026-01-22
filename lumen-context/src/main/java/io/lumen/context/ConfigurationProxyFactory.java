package io.lumen.context;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class ConfigurationProxyFactory {

    public static <T> T createProxy(Class<T> configClass, LightContainer container) {
        try {
            return new ByteBuddy()
                    .subclass(configClass)
                    .method(ElementMatchers.isAnnotatedWith(io.lumen.context.annotation.Light.class))
                    .intercept(MethodDelegation.to(new ByteBuddyInterceptor(container)))
                    .make()
                    .load(configClass.getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException("ByteBuddy failed to create proxy for " + configClass.getName(), e);
        }
    }

    public static class ByteBuddyInterceptor {
        private final LightContainer container;

        public ByteBuddyInterceptor(LightContainer container) {
            this.container = container;
        }

        @RuntimeType
        public Object intercept(@Origin Method method,
                                @SuperCall Callable<?> clientProxy) throws Exception {

            String lightName = method.getName();
            LightInstance light = container.internals().getLightInstance(lightName);

            // LOGIC:
            // If the light exists and is READY, return the singleton.
            // If it's INSTANTIATING, this is the container calling the method for the first time.
            // We MUST call superCall.call() to actually run the 'return new X()' code.
            if (light != null && light.getState() == LightInstance.LightState.READY) {
                return light.getInstance();
            }
            return clientProxy.call();
        }
    }
}