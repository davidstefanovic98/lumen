package io.lumen.context;

import io.lumen.context.annotation.ComponentScan;
import io.lumen.core.LumenModule;
import io.lumen.core.component.LightContainer;

import java.util.ServiceLoader;

final class ModuleInitializer {

    private ModuleInitializer() {}

    static void initializeModules(Class<?> configClass, LightContainer container) {
        String[] basePackages = resolveBasePackages(configClass);

        ServiceLoader<LumenModule> loader =
                ServiceLoader.load(LumenModule.class);

        for (LumenModule module : loader) {
            module.init(container, basePackages);
        }
    }

    private static String[] resolveBasePackages(Class<?> configClass) {
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan scan = configClass.getAnnotation(ComponentScan.class);
            String[] basePackages = scan.basePackages();
            if (basePackages.length > 0) {
                return basePackages;
            }
        }
        return new String[] { configClass.getPackageName() };
    }
}
