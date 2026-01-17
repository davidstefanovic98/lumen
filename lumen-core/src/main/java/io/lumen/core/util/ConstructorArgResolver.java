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
                    if (entry.getKey().equals(dep)) elements.add(entry.getValue());
                }
                arg = ProxyFactory.createLazyCollection(container, elements);
            } else if (dep.getValueKey() != null && provider.canProvide(dep)) {
                // Non-light dependency, resolve via provider
                arg = provider.provide(dep);
            } else {
                LightInstance depLight = resolvedMap.get(dep);
                if (depLight != null) {
                    arg = depLight.getMetadata().getDefinition().isLazy()
                            ? ProxyFactory.createLazy(container, depLight, dep.getType())
                            : depLight.getInstance();
                } else if (dep.isRequired()) {
                    throw new LightInstantiationException(
                            "Required dependency not resolved: " + dep +
                                    " for light: " + light.getName()
                    );
                }
            }

            args.add(arg);
        }

        return args.toArray();
    }
}
