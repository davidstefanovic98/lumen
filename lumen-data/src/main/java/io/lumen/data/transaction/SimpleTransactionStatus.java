package io.lumen.data.transaction;

import jakarta.persistence.EntityManager;

public class SimpleTransactionStatus implements TransactionStatus {

    private final boolean newTransaction;
    private final boolean transactionStarted;
    private boolean rollbackOnly;
    private EntityManager suspendedEm;

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
}