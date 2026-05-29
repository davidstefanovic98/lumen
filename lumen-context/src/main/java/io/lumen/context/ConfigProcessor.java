package io.lumen.context;

import io.lumen.context.annotation.Component;
import io.lumen.context.annotation.ComponentScan;
import io.lumen.context.annotation.Configuration;
import io.lumen.core.annotation.Light;
import io.lumen.core.annotation.Primary;
import io.lumen.core.annotation.Value;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightDefinition;
import io.lumen.core.component.LightFactory;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.proxy.ProxyFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import static io.lumen.core.util.ReflectionUtil.hasAnnotation;
import static io.lumen.core.util.Utils.stripPlaceholder;

/**
 * Processes a user-defined configuration class:
 * - Scans @Light methods and registers them
 * - Scans @ComponentScan annotations and registers packages
 */
class ConfigProcessor {

    private final LightContainer container;
    private final Logger logger = LoggerFactory.getLogger(ConfigProcessor.class);

    ConfigProcessor(LightContainer container, Class<?> configClass) {
        this.container = container;
        process(configClass);
    }

    /**
     * Process the configuration class.
     *
     * @param configClass The user-defined configuration class
     */
    void process(Class<?> configClass) {
        try {
            if (!isConfiguration(configClass)) {
                logger.warn("Class {} is not annotated with @Configuration. Skipping light method processing.", configClass.getName());
                return;
            }

            if (container.hasLight(configClass)) {
                return;
            }

            Object configInstance = instantiateConfigClass(configClass);
            for (LightProcessor processor : container.getPostProcessors()) {
                configInstance = processor.afterInstantiation(null, configInstance);
            }
            container.registerExternalInstance(configClass, configInstance);

            if (configClass.isAnnotationPresent(ComponentScan.class)) {
                ComponentScan scan = configClass.getAnnotation(ComponentScan.class);
                String[] packages = scan.basePackages().length > 0
                        ? scan.basePackages()
                        : new String[] { configClass.getPackageName() };
                scanPackage(packages);
            } else {
                scanPackage(new String[] { configClass.getPackageName() });
            }

            for (Method method : configClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Light.class)) {
                    if (Modifier.isFinal(method.getModifiers()) || Modifier.isPrivate(method.getModifiers())) {
                        throw new RuntimeException("Method " + method.getName() + " in @Configuration must not be private or final.");
                    }
                    processLightMethod(method, configInstance);
                }
            }
        } catch (Throwable t) {
            logger.error("Failed to process configuration class: " + configClass, t);
            throw new RuntimeException("Failed to process configuration class: " + configClass, t);
        }
    }

    private boolean isConfiguration(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Configuration.class)) return true;
        for (var ann : clazz.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(Configuration.class)) return true;
        }
        return false;
    }

    private Object instantiateConfigClass(Class<?> configClass) {
        if (isConfiguration(configClass)) {
            return ProxyFactory.createConfigurationProxy(configClass, container);
        }

        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate config class: " + configClass, e);
        }
    }

    private void processLightMethod(Method method, Object configInstance) {
        method.setAccessible(true);
        boolean isPrimary = method.isAnnotationPresent(Primary.class);
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
                method.getReturnType(),
                isPrimary
        );
        container.registerFactory(def.getName(), method, factory, method.getReturnType(), isPrimary);
        def.setExecutable(method);
    }

    private void scanPackage(String... basePackage) {
        PackageScanner.scan(basePackage).forEach(clazz -> {
            if (clazz.isAnnotation())
                return;

            if (clazz.isInterface())
                return;

            if (clazz.isAnnotationPresent(Configuration.class)) {
                process(clazz);
            }

            if (hasAnnotation(clazz, Component.class)) {
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
                args[i] = container.internals().getLightByType(param.getType());
            }
        }

        return args;
    }
}

