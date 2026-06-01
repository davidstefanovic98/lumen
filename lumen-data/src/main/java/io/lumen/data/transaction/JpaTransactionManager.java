package io.lumen.data.transaction;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.data.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;

public class JpaTransactionManager implements LumenTransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(JpaTransactionManager.class);

    private final EntityManagerFactory emf;

    public JpaTransactionManager(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public TransactionStatus getTransaction(Transactional annotation) {
        Propagation propagation = annotation.propagation();
        boolean     readOnly    = annotation.readOnly();
        Isolation   isolation   = annotation.isolation();
        boolean     hasActive   = EntityManagerHolder.hasActive();

        return switch (propagation) {
            case REQUIRED -> {
                if (hasActive) yield new SimpleTransactionStatus(false, true);
                yield beginNew(readOnly, isolation, null);
            }
            case REQUIRES_NEW -> {
                EntityManager suspended = hasActive ? EntityManagerHolder.get() : null;
                if (hasActive) EntityManagerHolder.clear();
                yield beginNew(readOnly, isolation, suspended);
            }
            case SUPPORTS -> {
                if (hasActive) yield new SimpleTransactionStatus(false, true);
                EntityManager em = emf.createEntityManager();
                EntityManagerHolder.set(em);
                yield new SimpleTransactionStatus(true, false);
            }
            case NOT_SUPPORTED -> {
                EntityManager suspended = hasActive ? EntityManagerHolder.get() : null;
                if (hasActive) EntityManagerHolder.clear();
                SimpleTransactionStatus s = new SimpleTransactionStatus(false, false);
                s.setSuspendedEm(suspended);
                yield s;
            }
            case MANDATORY -> {
                if (!hasActive) throw new IllegalTransactionStateException(
                        "@Transactional(propagation=MANDATORY) requires an active transaction");
                yield new SimpleTransactionStatus(false, true);
            }
            case NEVER -> {
                if (hasActive) throw new IllegalTransactionStateException(
                        "@Transactional(propagation=NEVER) must not be called within a transaction");
                yield new SimpleTransactionStatus(false, false);
            }
        };
    }

    @Override
    public void commit(TransactionStatus status) {
        SimpleTransactionStatus sts = (SimpleTransactionStatus) status;
        if (sts.isNewTransaction()) {
            EntityManager em = EntityManagerHolder.get();
            try {
                if (sts.isTransactionStarted()) {
                    if (sts.isRollbackOnly()) em.getTransaction().rollback();
                    else em.getTransaction().commit();
                }
            } finally {
                EntityManagerHolder.clear();
                em.close();
            }
        }
        resume(sts);
    }

    @Override
    public void rollback(TransactionStatus status) {
        SimpleTransactionStatus sts = (SimpleTransactionStatus) status;
        if (sts.isNewTransaction()) {
            EntityManager em = EntityManagerHolder.get();
            try {
                if (sts.isTransactionStarted() && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } finally {
                EntityManagerHolder.clear();
                em.close();
            }
        }
        resume(sts);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SimpleTransactionStatus beginNew(boolean readOnly, Isolation isolation,
                                             EntityManager suspended) {
        EntityManager em = emf.createEntityManager();
        EntityManagerHolder.set(em);
        applyIsolation(em, isolation);
        em.getTransaction().begin();
        if (readOnly) em.setFlushMode(FlushModeType.COMMIT);
        SimpleTransactionStatus status = new SimpleTransactionStatus(true, true);
        status.setSuspendedEm(suspended);
        return status;
    }

    private void resume(SimpleTransactionStatus status) {
        if (status.getSuspendedEm() != null) {
            EntityManagerHolder.set(status.getSuspendedEm());
        }
    }

    private void applyIsolation(EntityManager em, Isolation isolation) {
        if (isolation == Isolation.DEFAULT) return;
        try {
            org.hibernate.Session session = em.unwrap(org.hibernate.Session.class);
            final int level = isolation.jdbcLevel();
            session.doWork(conn -> conn.setTransactionIsolation(level));
        } catch (Exception e) {
            logger.debug("Could not apply isolation level {} — {}", isolation, e.getMessage());
        }
    }
}