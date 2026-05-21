package io.lumen.data.repository.proxy;

import io.lumen.data.pageable.Page;
import io.lumen.data.pageable.PageImpl;
import io.lumen.data.pageable.Pageable;
import io.lumen.data.pageable.Sort;
import jakarta.persistence.EntityManagerFactory;

import java.lang.reflect.Method;
import java.util.List;

public class PagingAndSortingExecutor extends AbstractRepositoryExecutor {

    public PagingAndSortingExecutor(EntityManagerFactory emf, Class<?> entityClass) {
        super(emf, entityClass);
    }

    @Override
    public boolean canHandle(Method method, Object[] args) {
        if (!"findAll".equals(method.getName())) return false;
        if (args == null || args.length == 0) return true;
        return args[0] instanceof Sort || args[0] instanceof Pageable;
    }

    @Override
    public Object invoke(Method method, Object[] args) {
        if (args == null || args.length == 0) return findAll();
        if (args[0] instanceof Pageable pageable) return findAllPaged(pageable);
        if (args[0] instanceof Sort sort) return findAllSorted(sort);
        throw new UnsupportedOperationException();
    }

    private List<?> findAll() {
        return inTransaction(() -> em().createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                .getResultList());
    }

    private List<?> findAllSorted(Sort sort) {
        return inTransaction(() -> em().createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e" + orderByClause(sort), entityClass)
                .getResultList());
    }

    private Page<?> findAllPaged(Pageable pageable) {
        return inTransaction(() -> {
            long total = em().createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class)
                    .getSingleResult();
            List<?> content = em().createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e" + orderByClause(pageable.getSort()), entityClass)
                    .setFirstResult((int) pageable.getOffset())
                    .setMaxResults(pageable.getPageSize())
                    .getResultList();
            return new PageImpl<>(content, pageable, total);
        });
    }
}