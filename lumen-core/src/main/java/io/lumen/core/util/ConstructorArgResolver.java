package io.lumen.core.util;

import io.lumen.core.component.Dependency;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.DependencyProvider;
import io.lumen.core.exception.LightInstantiationException;
import io.lumen.core.proxy.ProxyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConstructorArgResolver {

    private ConstructorArgResolver() {}

    /**
     * Collect constructor arguments from resolved dependencies.
     * Automatically creates proxies for dependencies that are not yet instantiated
     * to resolve circular and ordering issues.
     */
    public static Object[] resolve(LightInstance light, LightContainer container, DependencyProvider provider) {
        List<Dependency> constructorDeps = light.getMetadata().getConstructorDeps();
        Map<Dependency, LightInstance> resolvedMap = light.getResolvedDependencyMap();

        List<Object> args = new ArrayList<>();

        for (Dependency dep : constructorDeps) {
            Object arg = null;

            if (dep.isCollection()) {
                List<LightInstance> elements = new ArrayList<>();
                for (Map.Entry<Dependency, LightInstance> entry : resolvedMap.entrySet()) {
                    if (entry.getKey().equals(dep)) {
                        elements.add(entry.getValue());
                    }
                }
                arg = ProxyFactory.createLazyCollection(container, elements);
            }

            else if (dep.getValueKey() != null && provider.canProvide(dep)) {
                arg = provider.provide(dep);
            }

            else {
                LightInstance depLight = resolvedMap.get(dep);

                if (depLight != null) {
                    // A) If the dependency is explicitly @Lazy -> Proxy it.
                    // B) If the target Light is marked @Lazy -> Proxy it.
                    // C) If the instance is NULL (not yet built) -> Proxy it (Auto-Lazy).
                    Object instance = depLight.getInstance();
                    boolean shouldProxy = dep.isLazy() ||
                            depLight.getMetadata().getDefinition().isLazy() ||
                            instance == null;

                    if (shouldProxy) {
                        arg = ProxyFactory.createLazy(container, depLight, dep.getType());
                    } else {
                        arg = instance;
                    }

                    if (arg == null && dep.isRequired()) {
                        throw new LightInstantiationException(
                                "Dependency " + dep.getType().getSimpleName() +
                                        " for " + light.getName() + " could not be instantiated or proxied.");
                    }
                } else if (dep.isRequired()) {
                    throw new LightInstantiationException("Required dependency not found in resolved map: " + dep);
                }
            }

            args.add(arg);
        }

        return args.toArray();
    }
}
