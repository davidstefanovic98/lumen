package io.lumen.data.repository.proxy;

import jakarta.persistence.EntityManagerFactory;

import java.lang.reflect.Method;

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
                    inTransaction(() -> { for (Object e : (Iterable<?>) args[0]) deleteEntity(e); em().flush(); return null; });
                yield null;
            }
            case "getReferenceById" -> inTransaction(() -> em().getReference(entityClass, args[0]));
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }
}