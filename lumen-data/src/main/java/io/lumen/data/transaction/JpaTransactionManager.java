package io.lumen.data.transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;

public class JpaTransactionManager implements LumenTransactionManager {

    private final EntityManagerFactory emf;

    public JpaTransactionManager(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public TransactionStatus getTransaction(boolean readOnly) {
        if (EntityManagerHolder.hasActive()) {
            return new SimpleTransactionStatus(false);
        }
        EntityManager em = emf.createEntityManager();
        EntityManagerHolder.set(em);
        em.getTransaction().begin();
        if (readOnly) em.setFlushMode(FlushModeType.COMMIT);
        return new SimpleTransactionStatus(true);
    }

    @Override
    public void commit(TransactionStatus status) {
        if (!status.isNewTransaction()) return;
        EntityManager em = EntityManagerHolder.get();
        try {
            if (status.isRollbackOnly()) em.getTransaction().rollback();
            else em.getTransaction().commit();
        } finally {
            EntityManagerHolder.clear();
            em.close();
        }
    }

    @Override
    public void rollback(TransactionStatus status) {
        if (!status.isNewTransaction()) return;
        EntityManager em = EntityManagerHolder.get();
        try {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            EntityManagerHolder.clear();
            em.close();
        }
    }
}