package io.lumen.data.repository.proxy;

import jakarta.persistence.EntityManagerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class JpaRepositoryExecutor extends AbstractRepositoryExecutor {

    public JpaRepositoryExecutor(EntityManagerFactory emf, Class<?> entityClass) {
        super(emf, entityClass);
    }

    @Override
    public boolean canHandle(Method method, Object[] args) {
        return switch (method.getName()) {
            case "flush", "saveAndFlush", "saveAllAndFlush", "deleteAllInBatch", "getReferenceById" -> true;
            default -> false;
        };
    }

    @Override
    public Object invoke(Method method, Object[] args) {
        return switch (method.getName()) {
            case "flush"           -> { inTransaction(() -> { em().flush(); return null; }); yield null; }
            case "saveAndFlush"    -> inTransaction(() -> { Object r = em().merge(args[0]); em().flush(); return r; });
            case "saveAllAndFlush" -> inTransaction(() -> { var r = saveAll((Iterable<?>) args[0]); em().flush(); return r; });
            case "deleteAllInBatch" -> {
                if (args == null || args.length == 0)
                    inTransaction(() -> { em().createQuery("DELETE FROM " + entityClass.getSimpleName() + " e").executeUpdate(); return null; });
                else
                    inTransaction(() -> { deleteAllInBatch((Iterable<?>) args[0]); return null; });
                yield null;
            }
            case "getReferenceById" -> inTransaction(() -> em().getReference(entityClass, args[0]));
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }

    /**
     * Unlike {@code CrudRepositoryExecutor.deleteAll(Iterable)}, which loads and removes each
     * entity individually to preserve cascade/lifecycle callbacks (matching Spring Data's own
     * split between {@code deleteAll} and {@code deleteAllInBatch}), this is the batched variant:
     * a single bulk {@code DELETE ... WHERE id IN (...)} that bypasses the persistence context
     * entirely — no cascades, no {@code @PreRemove}/{@code @PostRemove}, but one round trip
     * regardless of collection size.
     */
    private void deleteAllInBatch(Iterable<?> entities) {
        List<Object> ids = new ArrayList<>();
        for (Object entity : entities) {
            ids.add(emf.getPersistenceUnitUtil().getIdentifier(entity));
        }
        if (ids.isEmpty()) return;

        em().createQuery("DELETE FROM " + entityClass.getSimpleName() + " e WHERE e." + idAttributeName() + " IN :ids")
                .setParameter("ids", ids)
                .executeUpdate();
    }

    private String idAttributeName() {
        var entityType = em().getMetamodel().entity(entityClass);
        return entityType.getId(entityType.getIdType().getJavaType()).getName();
    }
}