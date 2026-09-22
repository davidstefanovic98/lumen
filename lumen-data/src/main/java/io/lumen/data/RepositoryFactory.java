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
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Walks the full interface hierarchy above {@code repoInterface}, substituting type
     * variables as it goes, to find where {@code Repository<T, ID>} is concretely bound — not
     * just {@code repoInterface}'s own directly declared interfaces. A custom intermediate
     * interface can bind the entity type one or more levels up and then be extended raw (e.g.
     * {@code interface UserOps extends BaseRepo<User> {}} then
     * {@code interface UserRepository extends UserOps {}}); in that case
     * {@code UserRepository.getGenericInterfaces()} alone isn't even a {@link ParameterizedType},
     * so a single-level check finds nothing.
     */
    Class<?> resolveEntityClass(Class<?> repoInterface) {
        Class<?> entityClass = findEntityType(repoInterface, Map.of());
        if (entityClass == null) {
            throw new IllegalArgumentException("Could not resolve entity type for " + repoInterface.getName());
        }
        return entityClass;
    }

    /**
     * @param typeVariables substitutions for {@code node}'s own type variables, contributed by
     *                      whichever subinterface referenced {@code node} on the way down
     */
    private Class<?> findEntityType(Class<?> node, Map<TypeVariable<?>, Type> typeVariables) {
        for (Type supertype : node.getGenericInterfaces()) {
            Class<?> rawSupertype;
            Map<TypeVariable<?>, Type> childBindings = new HashMap<>();

            if (supertype instanceof ParameterizedType pt) {
                rawSupertype = (Class<?>) pt.getRawType();
                TypeVariable<?>[] params = rawSupertype.getTypeParameters();
                Type[] actualArgs = pt.getActualTypeArguments();
                for (int i = 0; i < params.length; i++) {
                    childBindings.put(params[i], resolveTypeVariable(actualArgs[i], typeVariables));
                }
            } else if (supertype instanceof Class<?> rawIntf) {
                rawSupertype = rawIntf; // extended raw — no further substitutions to contribute
            } else {
                continue;
            }

            if (rawSupertype.equals(Repository.class)) {
                Type entityType = childBindings.get(Repository.class.getTypeParameters()[0]);
                if (entityType instanceof Class<?> entityClass) return entityClass;
            } else if (Repository.class.isAssignableFrom(rawSupertype)) {
                Class<?> found = findEntityType(rawSupertype, childBindings);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Type resolveTypeVariable(Type type, Map<TypeVariable<?>, Type> typeVariables) {
        if (type instanceof TypeVariable<?> typeVariable) {
            Type resolved = typeVariables.get(typeVariable);
            return resolved == null ? type : resolveTypeVariable(resolved, typeVariables);
        }
        return type;
    }
}
