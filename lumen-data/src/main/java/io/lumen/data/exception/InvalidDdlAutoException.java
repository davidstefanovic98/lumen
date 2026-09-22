package io.lumen.data.exception;

public class InvalidDdlAutoException extends RuntimeException {
    public InvalidDdlAutoException(String message) {
        super(message);
    }
}