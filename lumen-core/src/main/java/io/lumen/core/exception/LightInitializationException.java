package io.lumen.core.exception;

public class LightInitializationException extends RuntimeException {

    public LightInitializationException(String message) {
        super(message);
    }

    public LightInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
