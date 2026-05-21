package io.lumen.data;

import io.lumen.core.component.LightContainer;
import io.lumen.core.proxy.ProxyFactory;
import io.lumen.core.proxy.ProxyProvider;
import io.lumen.data.query.CompositeQueryParser;
import io.lumen.data.repository.Repository;
import io.lumen.data.repository.proxy.JpaRepositoryInterceptor;
import jakarta.persistence.EntityManagerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class RepositoryFactory {
    private final LightContainer container;

    public RepositoryFactory(LightContainer container) {
        this.container = container;
    }

    public <T> T create(Class<T> repoInterface) {
        EntityManagerFactory emf = container.internals().getLightByType(EntityManagerFactory.class);
        Class<?> entityClass = resolveEntityClass(repoInterface);
        var interceptor = new JpaRepositoryInterceptor(emf, entityClass, new CompositeQueryParser());
        return ProxyFactory.createInterfaceProxy(repoInterface, List.of(interceptor));
    }

    private Class<?> resolveEntityClass(Class<?> repoInterface) {
        for (Type intf : repoInterface.getGenericInterfaces()) {
            if (intf instanceof ParameterizedType pt) {
                if (Repository.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                    return (Class<?>) pt.getActualTypeArguments()[0];
                }
            }
        }
        throw new IllegalArgumentException("Could not resolve entity type for " + repoInterface.getName());
    }
}
