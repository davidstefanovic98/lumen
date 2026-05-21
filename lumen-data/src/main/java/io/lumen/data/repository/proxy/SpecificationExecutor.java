package io.lumen.data.repository.proxy;

import io.lumen.data.pageable.Page;
import io.lumen.data.pageable.PageImpl;
import io.lumen.data.pageable.Pageable;
import io.lumen.data.pageable.Sort;
import io.lumen.data.specification.Specification;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class SpecificationExecutor extends AbstractRepositoryExecutor {

    public SpecificationExecutor(EntityManagerFactory emf, Class<?> entityClass) {
        super(emf, entityClass);
    }

    @Override
    public boolean canHandle(Method method, Object[] args) {
        return switch (method.getName()) {
            case "findOne", "exists" -> true;
            case "count", "delete", "findAll" -> args != null && args.length > 0 && args[0] instanceof Specification<?>;
            default -> false;
        };
    }

    @Override
    public Object invoke(Method method, Object[] args) {
        return switch (method.getName()) {
            case "findOne" -> findOne(cast(args[0]));
            case "exists"  -> exists(cast(args[0]));
            case "count"   -> count(cast(args[0]));
            case "delete"  -> { inTransaction(() -> { delete(cast(args[0])); return null; }); yield null; }
            case "findAll" -> {
                Specification<Object> spec = cast(args[0]);
                if (args.length == 1)              yield findAll(spec, Sort.unsorted());
                if (args[1] instanceof Pageable p) yield findAllPaged(spec, p);
                if (args[1] instanceof Sort s)     yield findAll(spec, s);
                throw new UnsupportedOperationException();
            }
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }

    private Optional<?> findOne(Specification<Object> spec) {
        return inTransaction(() -> {
            CriteriaBuilder cb = em().getCriteriaBuilder();
            CriteriaQuery<Object> query = cb.createQuery(entityType());
            Root<Object> root = query.from(entityType());
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) query.where(predicate);
            try {
                return Optional.ofNullable(em().createQuery(query).getSingleResult());
            } catch (NoResultException e) {
                return Optional.empty();
            }
        });
    }

    private List<?> findAll(Specification<Object> spec, Sort sort) {
        return inTransaction(() -> {
            CriteriaBuilder cb = em().getCriteriaBuilder();
            CriteriaQuery<Object> query = cb.createQuery(entityType());
            Root<Object> root = query.from(entityType());
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) query.where(predicate);
            if (sort.isSorted()) query.orderBy(toJpaOrders(sort, root, cb));
            return em().createQuery(query).getResultList();
        });
    }

    private Page<?> findAllPaged(Specification<Object> spec, Pageable pageable) {
        return inTransaction(() -> {
            long total = count(spec);
            CriteriaBuilder cb = em().getCriteriaBuilder();
            CriteriaQuery<Object> query = cb.createQuery(entityType());
            Root<Object> root = query.from(entityType());
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) query.where(predicate);
            if (pageable.getSort().isSorted()) query.orderBy(toJpaOrders(pageable.getSort(), root, cb));
            List<?> content = em().createQuery(query)
                    .setFirstResult((int) pageable.getOffset())
                    .setMaxResults(pageable.getPageSize())
                    .getResultList();
            return new PageImpl<>(content, pageable, total);
        });
    }

    private long count(Specification<Object> spec) {
        return inTransaction(() -> {
            CriteriaBuilder cb = em().getCriteriaBuilder();
            CriteriaQuery<Long> query = cb.createQuery(Long.class);
            Root<Object> root = query.from(entityType());
            query.select(cb.count(root));
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) query.where(predicate);
            return em().createQuery(query).getSingleResult();
        });
    }

    private boolean exists(Specification<Object> spec) {
        return count(spec) > 0;
    }

    private void delete(Specification<Object> spec) {
        CriteriaBuilder cb = em().getCriteriaBuilder();
        CriteriaDelete<Object> query = cb.createCriteriaDelete(entityType());
        Root<Object> root = query.from(entityType());
        Predicate predicate = spec.toPredicate(root, null, cb);
        if (predicate != null) query.where(predicate);
        em().createQuery(query).executeUpdate();
    }
}