package io.lumen.data.transaction;

import jakarta.persistence.EntityManager;

public class SimpleTransactionStatus implements TransactionStatus {

    private final boolean newTransaction;
    private final boolean transactionStarted;
    private boolean rollbackOnly;
    private EntityManager suspendedEm;
    private Integer previousIsolationLevel;

    /**
     * Convenience: owning a new transaction implies one was started.
     */
    public SimpleTransactionStatus(boolean newTransaction) {
        this(newTransaction, newTransaction);
    }

    public SimpleTransactionStatus(boolean newTransaction, boolean transactionStarted) {
        this.newTransaction = newTransaction;
        this.transactionStarted = transactionStarted;
    }

    @Override
    public boolean isNewTransaction() {
        return newTransaction;
    }

    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    /**
     * Whether a JPA transaction was actually begun (false for SUPPORTS/NOT_SUPPORTED/NEVER).
     */
    public boolean isTransactionStarted() {
        return transactionStarted;
    }

    public EntityManager getSuspendedEm() {
        return suspendedEm;
    }

    public void setSuspendedEm(EntityManager em) {
        this.suspendedEm = em;
    }

    /**
     * The connection's transaction isolation level before {@code JpaTransactionManager} changed
     * it for this transaction (non-null only when a non-DEFAULT {@code Isolation} was requested
     * and actually differed from what was already set). Restored when this transaction completes,
     * so a borrowed connection never carries a non-default isolation level back to whatever hands
     * out the next one — a raw JDBC connection or a basic connection provider doesn't reset this
     * itself the way a real pool like HikariCP does.
     */
    public Integer getPreviousIsolationLevel() {
        return previousIsolationLevel;
    }

    public void setPreviousIsolationLevel(Integer previousIsolationLevel) {
        this.previousIsolationLevel = previousIsolationLevel;
    }
}