package io.lumen.data.transaction;

public interface LumenTransactionManager {

    /**
     * Begin or join a transaction. Returns a status describing whether this
     * call is the transaction owner (new transaction) or a participant (joined).
     */
    TransactionStatus getTransaction(boolean readOnly);

    void commit(TransactionStatus status);

    void rollback(TransactionStatus status);
}