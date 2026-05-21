package io.lumen.data.transaction;

public enum Propagation {
    /**
     * Join an existing transaction if active; otherwise start a new one.
     */
    REQUIRED,

    /**
     * Always start a new transaction, suspending any existing one.
     * NOTE: requires separate EntityManager instances; not fully supported with a
     * single shared EntityManager — behaves as REQUIRED in that case.
     */
    REQUIRES_NEW
}