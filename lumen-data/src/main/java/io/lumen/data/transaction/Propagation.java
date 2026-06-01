package io.lumen.data.transaction;

public enum Propagation {
    /** Join existing transaction; start a new one if none is active. */
    REQUIRED,

    /** Always start a new transaction, suspending any existing one. */
    REQUIRES_NEW,

    /** Join existing transaction; run non-transactionally if none is active. */
    SUPPORTS,

    /** Suspend any existing transaction and run non-transactionally. */
    NOT_SUPPORTED,

    /** Join existing transaction; throw if none is active. */
    MANDATORY,

    /** Run non-transactionally; throw if a transaction is active. */
    NEVER
}