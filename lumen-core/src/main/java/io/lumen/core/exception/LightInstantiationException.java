package io.lumen.core.exception;

public class LightInstantiationException extends RuntimeException {

    public LightInstantiationException(String message) {
      super(message);
    }

    public LightInstantiationException(String message, Throwable cause) {
      super(message, cause);
    }
}
