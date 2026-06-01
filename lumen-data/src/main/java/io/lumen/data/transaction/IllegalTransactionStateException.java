package io.lumen.data.transaction;

public class IllegalTransactionStateException extends RuntimeException {
    public IllegalTransactionStateException(String message) {
        super(message);
    }
}