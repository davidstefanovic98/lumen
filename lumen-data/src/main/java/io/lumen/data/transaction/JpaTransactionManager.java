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
                restoreIsolation(em, sts.getPreviousIsolationLevel());
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
                restoreIsolation(em, sts.getPreviousIsolationLevel());
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
        Integer previousIsolationLevel = applyIsolation(em, isolation);
        em.getTransaction().begin();
        if (readOnly) em.setFlushMode(FlushModeType.COMMIT);
        SimpleTransactionStatus status = new SimpleTransactionStatus(true, true);
        status.setSuspendedEm(suspended);
        status.setPreviousIsolationLevel(previousIsolationLevel);
        return status;
    }

    private void resume(SimpleTransactionStatus status) {
        if (status.getSuspendedEm() != null) {
            EntityManagerHolder.set(status.getSuspendedEm());
        }
    }

    /**
     * Changes the connection's isolation level and returns what it was before, so it can be
     * restored in {@link #restoreIsolation} once this transaction completes. Returning null means
     * "nothing to restore" (DEFAULT was requested, the level already matched, or the change
     * itself failed) — restoreIsolation treats that as a no-op.
     * <p>
     * This can't be left to whatever hands out connections: a real pool like HikariCP resets a
     * changed isolation level on its own when the connection is returned, but Hibernate's
     * built-in (non-Hikari) connection handling does not, so a SERIALIZABLE transaction here would
     * otherwise leak that isolation level into whichever transaction borrows the connection next.
     */
    private Integer applyIsolation(EntityManager em, Isolation isolation) {
        if (isolation == Isolation.DEFAULT) return null;
        try {
            org.hibernate.Session session = em.unwrap(org.hibernate.Session.class);
            final int level = isolation.jdbcLevel();
            final int[] previous = new int[1];
            session.doWork(conn -> {
                previous[0] = conn.getTransactionIsolation();
                conn.setTransactionIsolation(level);
            });
            return previous[0] == level ? null : previous[0];
        } catch (Exception e) {
            logger.debug("Could not apply isolation level {} — {}", isolation, e.getMessage());
            return null;
        }
    }

    private void restoreIsolation(EntityManager em, Integer previousLevel) {
        if (previousLevel == null) return;
        try {
            org.hibernate.Session session = em.unwrap(org.hibernate.Session.class);
            session.doWork(conn -> conn.setTransactionIsolation(previousLevel));
        } catch (Exception e) {
            logger.debug("Could not restore isolation level {} — {}", previousLevel, e.getMessage());
        }
    }
}