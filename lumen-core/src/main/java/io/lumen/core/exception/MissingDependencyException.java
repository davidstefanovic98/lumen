package io.lumen.core.exception;

public class MissingDependencyException extends RuntimeException {

    public MissingDependencyException(String message) {
        super(message);
    }

    public MissingDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
