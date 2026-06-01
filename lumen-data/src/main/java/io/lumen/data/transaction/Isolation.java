package io.lumen.data.transaction;

import java.sql.Connection;

public enum Isolation {
    /** Use the database's default isolation level. */
    DEFAULT(-1),

    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int jdbcLevel;

    Isolation(int jdbcLevel) {
        this.jdbcLevel = jdbcLevel;
    }

    public int jdbcLevel() {
        return jdbcLevel;
    }
}