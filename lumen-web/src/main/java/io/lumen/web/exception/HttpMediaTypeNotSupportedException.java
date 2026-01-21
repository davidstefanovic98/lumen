package io.lumen.web.exception;

public class HttpMediaTypeNotSupportedException extends RuntimeException {
    public HttpMediaTypeNotSupportedException(String message) {
        super(message);
    }
}
