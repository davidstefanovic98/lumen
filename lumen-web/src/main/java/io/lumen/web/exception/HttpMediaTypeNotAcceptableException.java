package io.lumen.web.exception;

public class HttpMediaTypeNotAcceptableException extends RuntimeException {
    public HttpMediaTypeNotAcceptableException(String message) {
        super(message);
    }
}
