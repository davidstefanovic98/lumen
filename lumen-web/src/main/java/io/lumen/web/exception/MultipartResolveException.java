package io.lumen.web.exception;

public class MultipartResolveException extends RuntimeException {

    public MultipartResolveException(String message) {
        super(message);
    }

    public MultipartResolveException(String message, Throwable cause) {
        super(message, cause);
    }
}
