package io.lumen.data.transaction;

public interface TransactionStatus {
    boolean isNewTransaction();
    boolean isRollbackOnly();
    void setRollbackOnly();
}