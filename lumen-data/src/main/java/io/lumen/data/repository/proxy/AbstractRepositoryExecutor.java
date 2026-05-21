package io.lumen.data.repository.proxy;

import io.lumen.data.pageable.Sort;
import io.lumen.data.transaction.EntityManagerHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

abstract class AbstractRepositoryExecutor implements RepositoryFragment {
    protected final EntityManagerFactory emf;
    protected final Class<?> entityClass;

    protected AbstractRepositoryExecutor(EntityManagerFactory emf, Class<?> entityClass) {
        this.emf = emf;
        this.entityClass = entityClass;
    }

    protected EntityManager em() {
        EntityManager em = EntityManagerHolder.get();
        if (em == null)
            throw new IllegalStateException("No EntityManager bound to thread — must be called within inTransaction()");
        return em;
    }

    protected <T> T inTransaction(Callable<T> action) {
        if (EntityManagerHolder.hasActive()) {
            // Join the transaction already started (e.g. by @Transactional service method)
            try {
                return action.call();
            } catch (Exception e) {
                throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        }

        // Own the transaction — create a fresh EntityManager for this unit of work
        EntityManager em = emf.createEntityManager();
        EntityManagerHolder.set(em);
        em.getTransaction().begin();
        try {
            T result = action.call();
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
        } finally {
            EntityManagerHolder.clear();
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T cast(Object o) { return (T) o; }

    @SuppressWarnings("unchecked")
    protected Class<Object> entityType() { return (Class<Object>) entityClass; }

    protected String orderByClause(Sort sort) {
        if (!sort.isSorted()) return "";
        return " ORDER BY " + sort.getOrders().stream()
                .map(o -> "e." + o.getProperty() + " " + o.getDirection().name())
                .collect(Collectors.joining(", "));
    }

    protected List<Order> toJpaOrders(Sort sort, Root<?> root, CriteriaBuilder cb) {
        return sort.getOrders().stream()
                .map(o -> o.getDirection() == Sort.Direction.ASC
                        ? cb.asc(root.get(o.getProperty()))
                        : cb.desc(root.get(o.getProperty())))
                .toList();
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> saveAll(Iterable<?> entities) {
        List<T> result = new ArrayList<>();
        for (Object entity : entities) result.add((T) em().merge(entity));
        return result;
    }

    protected void deleteEntity(Object entity) {
        EntityManager em = em();
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }
}
