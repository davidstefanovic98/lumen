package io.lumen.core.exception;

public class MultipleLightFoundException extends RuntimeException {
    public MultipleLightFoundException(String message) {
      super(message);
    }

    public MultipleLightFoundException(String message, Throwable cause) {
      super(message, cause);
    }
}
