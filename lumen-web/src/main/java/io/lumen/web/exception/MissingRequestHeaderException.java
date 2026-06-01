package io.lumen.web.exception;

public class MissingRequestHeaderException extends RuntimeException {
    public MissingRequestHeaderException(String message) {
        super(message);
    }
}