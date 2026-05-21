package io.lumen.context;

import io.lumen.context.annotation.ComponentScan;
import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

final class ModuleInitializer {

    private ModuleInitializer() {}

    static void initializeModules(Class<?> configClass, LightContainer container) {
        String[] basePackages = resolveBasePackages(configClass);

        List<LumenModule> modules = new ArrayList<>();
        ServiceLoader.load(LumenModule.class).forEach(modules::add);

        // Respect @Order — modules without it run last
        modules.sort(Comparator.comparingInt(m -> {
            Order order = m.getClass().getAnnotation(Order.class);
            return order != null ? order.value() : Integer.MAX_VALUE;
        }));

        for (LumenModule module : modules) {
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
