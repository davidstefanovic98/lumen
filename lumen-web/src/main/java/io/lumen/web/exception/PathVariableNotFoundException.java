package io.lumen.web.exception;

public class PathVariableNotFoundException extends RuntimeException {
    public PathVariableNotFoundException(String message) {
        super(message);
    }
}