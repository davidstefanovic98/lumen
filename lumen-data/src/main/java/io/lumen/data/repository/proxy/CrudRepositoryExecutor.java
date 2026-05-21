package io.lumen.data.repository.proxy;

import io.lumen.data.specification.Specification;
import jakarta.persistence.EntityManagerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CrudRepositoryExecutor extends AbstractRepositoryExecutor {

    public CrudRepositoryExecutor(EntityManagerFactory emf, Class<?> entityClass) {
        super(emf, entityClass);
    }

    @Override
    public boolean canHandle(Method method, Object[] args) {
        return switch (method.getName()) {
            case "save", "saveAll", "findById", "existsById", "findAllById", "deleteById", "deleteAll" -> true;
            case "count"  -> args == null || args.length == 0;
            case "delete" -> args != null && args.length > 0 && !(args[0] instanceof Specification<?>);
            default -> false;
        };
    }

    @Override
    public Object invoke(Method method, Object[] args) {
        return switch (method.getName()) {
            case "save"        -> inTransaction(() -> em().merge(args[0]));
            case "saveAll"     -> inTransaction(() -> saveAll((Iterable<?>) args[0]));
            case "findById"    -> inTransaction(() -> Optional.ofNullable(em().find(entityClass, args[0])));
            case "existsById"  -> inTransaction(() -> em().find(entityClass, args[0]) != null);
            case "findAllById" -> inTransaction(() -> findAllById((Iterable<?>) args[0]));
            case "count"       -> inTransaction(() -> count());
            case "deleteById"  -> { inTransaction(() -> { deleteById(args[0]); return null; }); yield null; }
            case "delete"      -> { inTransaction(() -> { deleteEntity(args[0]); return null; }); yield null; }
            case "deleteAll"   -> {
                if (args == null || args.length == 0)
                    inTransaction(() -> { em().createQuery("DELETE FROM " + entityClass.getSimpleName() + " e").executeUpdate(); return null; });
                else
                    inTransaction(() -> { for (Object e : (Iterable<?>) args[0]) deleteEntity(e); return null; });
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }

    private List<?> findAllById(Iterable<?> ids) {
        List<Object> result = new ArrayList<>();
        for (Object id : ids) {
            Object entity = em().find(entityClass, id);
            if (entity != null) result.add(entity);
        }
        return result;
    }

    private long count() {
        return em().createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }

    private void deleteById(Object id) {
        Object entity = em().find(entityClass, id);
        if (entity != null) em().remove(entity);
    }
}