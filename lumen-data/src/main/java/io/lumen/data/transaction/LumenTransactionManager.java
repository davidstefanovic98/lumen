package io.lumen.data.transaction;

import io.lumen.data.annotation.Transactional;

public interface LumenTransactionManager {

    /**
     * Begin, join, or suspend a transaction according to the annotation's
     * propagation and isolation settings.
     */
    TransactionStatus getTransaction(Transactional annotation);

    void commit(TransactionStatus status);

    void rollback(TransactionStatus status);
}