package io.lumen.data.transaction;

public class SimpleTransactionStatus implements TransactionStatus {
    private final boolean newTransaction;
    private boolean rollbackOnly;

    public SimpleTransactionStatus(boolean newTransaction) {
        this.newTransaction = newTransaction;
    }

    @Override public boolean isNewTransaction() { return newTransaction; }
    @Override public boolean isRollbackOnly()   { return rollbackOnly; }
    @Override public void setRollbackOnly()      { this.rollbackOnly = true; }
}