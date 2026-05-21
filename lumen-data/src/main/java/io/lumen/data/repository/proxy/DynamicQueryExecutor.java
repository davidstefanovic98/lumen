package io.lumen.data.repository.proxy;

import io.lumen.data.annotation.Param;
import io.lumen.data.pageable.Page;
import io.lumen.data.pageable.PageImpl;
import io.lumen.data.pageable.Pageable;
import io.lumen.data.query.QueryDescriptor;
import io.lumen.data.query.QueryParser;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicQueryExecutor extends AbstractRepositoryExecutor {
    private final QueryParser queryParser;

    private static final Pattern FROM_ALIAS = Pattern.compile("(?i)FROM\\s+\\w+\\s+(\\w+)");

    public DynamicQueryExecutor(EntityManagerFactory emf, Class<?> entityClass, QueryParser queryParser) {
        super(emf, entityClass);
        this.queryParser = queryParser;
    }

    @Override
    public boolean canHandle(Method method, Object[] args) {
        return true;
    }

    @Override
    public Object invoke(Method method, Object[] args) {
        QueryDescriptor descriptor = queryParser.parse(method, entityClass);

        if (descriptor.modifying()) {
            return inTransaction(() -> {
                var q = descriptor.isNative()
                        ? em().createNativeQuery(descriptor.query())
                        : em().createQuery(descriptor.query());
                bindParams(q, method, args, descriptor.namedParams());
                int affected = q.executeUpdate();
                Class<?> rt = method.getReturnType();
                if (rt == void.class || rt == Void.class) return null;
                if (rt == long.class || rt == Long.class) return (long) affected;
                return affected;
            });
        }

        if (descriptor.queryType() == QueryDescriptor.QueryType.COUNT) {
            return inTransaction(() -> {
                var q = em().createQuery(descriptor.query(), Long.class);
                bindParams(q, method, args, descriptor.namedParams());
                return q.getSingleResult();
            });
        }

        if (descriptor.queryType() == QueryDescriptor.QueryType.EXISTS) {
            return inTransaction(() -> {
                var q = em().createQuery(descriptor.query(), Long.class);
                bindParams(q, method, args, descriptor.namedParams());
                return q.getSingleResult() > 0;
            });
        }

        if (descriptor.queryType() == QueryDescriptor.QueryType.DELETE) {
            return inTransaction(() -> {
                var q = em().createQuery(descriptor.query());
                bindParams(q, method, args, descriptor.namedParams());
                int affected = q.executeUpdate();
                Class<?> rt = method.getReturnType();
                if (rt == void.class || rt == Void.class) return null;
                if (rt == long.class || rt == Long.class) return (long) affected;
                return affected;
            });
        }

        Class<?> returnType = method.getReturnType();
        if (Page.class.isAssignableFrom(returnType)) {
            return executePaged(descriptor, method, args);
        }

        return inTransaction(() -> {
            var query = descriptor.isNative()
                    ? em().createNativeQuery(descriptor.query(), entityClass)
                    : em().createQuery(descriptor.query(), entityClass);
            bindParams(query, method, args, descriptor.namedParams());

            if (Iterable.class.isAssignableFrom(returnType) || List.class.isAssignableFrom(returnType)) {
                return query.getResultList();
            }

            Object result = null;
            try {
                result = query.getSingleResult();
            } catch (NoResultException ignored) {}

            return returnType.equals(Optional.class) ? Optional.ofNullable(result) : result;
        });
    }

    private Page<?> executePaged(QueryDescriptor descriptor, Method method, Object[] args) {
        return inTransaction(() -> {
            Pageable pageable = findPageable(args);

            String countJpql = descriptor.countQuery() != null
                    ? descriptor.countQuery()
                    : deriveCountQuery(descriptor.query());
            var countQuery = em().createQuery(countJpql, Long.class);
            bindParams(countQuery, method, args, descriptor.namedParams());
            long total = countQuery.getSingleResult();

            String dataJpql = descriptor.query();
            if (pageable.getSort().isSorted() && !dataJpql.toUpperCase().contains("ORDER BY")) {
                dataJpql = dataJpql + orderByClause(pageable.getSort());
            }

            var dataQuery = descriptor.isNative()
                    ? em().createNativeQuery(dataJpql, entityClass)
                    : em().createQuery(dataJpql, entityClass);
            bindParams(dataQuery, method, args, descriptor.namedParams());
            dataQuery.setFirstResult((int) pageable.getOffset());
            dataQuery.setMaxResults(pageable.getPageSize());

            return new PageImpl<>(dataQuery.getResultList(), pageable, total);
        });
    }

    private Pageable findPageable(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Pageable p) return p;
            }
        }
        throw new IllegalArgumentException("No Pageable argument found");
    }

    private String deriveCountQuery(String jpql) {
        String without = jpql.replaceAll("(?i)\\s+ORDER\\s+BY.+$", "");
        Matcher m = FROM_ALIAS.matcher(without);
        String alias = m.find() ? m.group(1) : "e";
        return without.replaceAll("(?i)^\\s*SELECT\\s+.+?\\s+FROM\\s", "SELECT COUNT(" + alias + ") FROM ");
    }

    private void bindParams(jakarta.persistence.Query query, Method method, Object[] args, boolean namedParams) {
        if (args == null) return;
        if (namedParams) {
            var params = method.getParameters();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Pageable) continue;
                Param ann = params[i].getAnnotation(Param.class);
                String name = ann != null ? ann.value() : params[i].getName();
                query.setParameter(name, args[i]);
            }
        } else {
            int pos = 1;
            for (Object arg : args) {
                if (arg instanceof Pageable) continue;
                query.setParameter(pos++, arg);
            }
        }
    }
}