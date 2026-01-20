package io.lumen.web.exception;

public class AmbiguousMappingException extends RuntimeException {
    public AmbiguousMappingException(String message) {
        super(message);
    }
}
