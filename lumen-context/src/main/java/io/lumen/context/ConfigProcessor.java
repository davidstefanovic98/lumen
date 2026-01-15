package io.lumen.context;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightDefinition;
import io.lumen.core.component.LightFactory;
import io.lumen.context.annotations.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static io.lumen.context.util.Utils.stripPlaceholder;

/**
 * Processes a user-defined configuration class:
 * - Scans @Bean methods and registers them
 * - Scans @ComponentScan annotations and registers packages
 */
class ConfigProcessor {

    private final LightContainer container;

    ConfigProcessor(LightContainer container, Class<?> configClass) {
        this.container = container;
        process(configClass);
    }

    /**
     * Process the configuration class.
     * @param configClass The user-defined configuration class
     */
    void process(Class<?> configClass) {
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan scan = configClass.getAnnotation(ComponentScan.class);
            Arrays.stream(scan.basePackages()).forEach(this::scanPackage);
        }

        Object configInstance = instantiateConfigClass(configClass);
        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Light.class)) {
                processLightMethod(method, configInstance);
            }
        }
    }

    private Object instantiateConfigClass(Class<?> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate config class: " + configClass, e);
        }
    }

    private void processLightMethod(Method method, Object configInstance) {
        method.setAccessible(true);
        LightFactory factory = (ld) -> {
            try {
                Object[] args = resolveMethodDependencies(method, container);
                return method.invoke(configInstance, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        LightDefinition def = LightDefinition.fromFactory(
                method.getName(),
                method,
                factory,
                method.getReturnType()
        );

        container.registerFactory(def.getName(), method, factory, method.getReturnType());
        def.setExecutable(method);
    }

    private void scanPackage(String basePackage) {
        PackageScanner.scan(basePackage).forEach(clazz -> {
            if (clazz.isAnnotationPresent(Component.class) || clazz.isAnnotationPresent(Service.class)) {
                container.register(clazz);
            }
        });
    }

    private Object[] resolveMethodDependencies(Method method, LightContainer container) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            Value valueAnn = param.getAnnotation(Value.class);

            if (valueAnn != null) {
                String key = valueAnn.value();
                args[i] = container.getApplicationContext()
                        .getEnvironment()
                        .getProperty(stripPlaceholder(key), param.getType());
            } else {
                args[i] = container.getLight(param.getType());
            }
        }

        return args;
    }
}

