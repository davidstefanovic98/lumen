package io.lumen.core.proxy;

import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Factory for creating proxies for lazy or intercepted objects.
 */
public class ProxyFactory {

    /**
     * Creates a lazy collection proxy.
     * @param container The container to resolve the actual instances
     * @param elements The underlying LightInstances
     * @return A lazily-resolved list
     */
    public static List<?> createLazyCollection(LightContainer container, List<LightInstance> elements) {
        return new AbstractList<>() {
            @Override
            public Object get(int index) {
                return container.getLight(elements.get(index).getName());
            }

            @Override
            public int size() {
                return elements.size();
            }
        };
    }

    /**
     * Creates a lazy proxy for a single object.
     * The instance is only created/resolved when a method is called.
     */
    @SuppressWarnings("unchecked")
    public static <T> T createLazy(LightContainer container, LightInstance light, Class<T> type) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                new InvocationHandler() {
                    private Object delegate;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (delegate == null) {
                            delegate = container.getLight(light.getName());
                        }
                        return method.invoke(delegate, args);
                    }
                }
        );
    }

    /**
     * Creates a lazy proxy that resolves to a value provided by a Supplier.
     * Use this for "External" dependencies like ServletContext or Database Connections.
     */
    @SuppressWarnings("unchecked")
    public static <T> T createLazyFromSupplier(Class<T> type, Supplier<T> supplier) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    T delegate = supplier.get();
                    if (delegate == null) {
                        throw new IllegalStateException("Dependency of type " + type.getSimpleName() +
                                " is not yet available (Server not started?)");
                    }
                    return method.invoke(delegate, args);
                }
        );
    }
}
