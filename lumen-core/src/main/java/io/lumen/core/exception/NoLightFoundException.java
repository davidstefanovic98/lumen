package io.lumen.core.exception;

public class NoLightFoundException extends RuntimeException {
    public NoLightFoundException(String message) {
      super(message);
    }

    public NoLightFoundException(String message, Throwable cause) {
      super(message, cause);
    }
}
