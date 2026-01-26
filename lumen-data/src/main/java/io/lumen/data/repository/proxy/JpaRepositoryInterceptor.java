package io.lumen.data.repository.proxy;

import io.lumen.core.interceptor.MethodInterceptor;
import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.data.query.QueryDescriptor;
import io.lumen.data.query.QueryParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class JpaRepositoryInterceptor implements MethodInterceptor {
    private final EntityManager em;
    private final Class<?> entityClass;
    private final QueryParser queryParser;

    public JpaRepositoryInterceptor(EntityManager em, Class<?> entityClass, QueryParser queryParser) {
        this.em = em;
        this.entityClass = entityClass;
        this.queryParser = queryParser;
    }

    @Override
    public Object invoke(MethodInvocation invocation) {
        String name = invocation.getMethod().getName();
        Object[] args = invocation.getArguments();

        return switch (name) {
            case "save" -> {
                boolean active = em.getTransaction().isActive();
                if (!active)
                    em.getTransaction().begin();

                Object result = em.merge(args[0]);

                if (!active)
                    em.getTransaction().commit();

                yield result;
            }
            case "findById" -> Optional.ofNullable(em.find(entityClass, args[0]));
            case "findAll" -> em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass).getResultList();
            case "deleteById" -> {
                Object entity = em.find(entityClass, args[0]);
                if (entity != null) em.remove(entity);
                yield null;
            }
            default -> handleDynamicQuery(invocation.getMethod(), args);
        };
    }

    private Object handleDynamicQuery(Method method, Object[] args) {
        QueryDescriptor descriptor = queryParser.parse(method, entityClass);

        var query = descriptor.isNative()
                ? em.createNativeQuery(descriptor.query(), entityClass)
                : em.createQuery(descriptor.query(), entityClass);

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                query.setParameter(i + 1, args[i]);
            }
        }

        Class<?> returnType = method.getReturnType();

        if (Iterable.class.isAssignableFrom(returnType) || List.class.isAssignableFrom(returnType)) {
            return query.getResultList();
        }

        Object result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException ignored) {}

        if (returnType.equals(Optional.class)) {
            return Optional.ofNullable(result);
        }

        return result;
    }
}
