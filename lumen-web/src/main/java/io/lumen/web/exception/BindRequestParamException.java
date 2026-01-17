package io.lumen.web.exception;

public class BindRequestParamException extends RuntimeException {

    public BindRequestParamException(String message) {
        super(message);
    }

    public BindRequestParamException(String message, Throwable cause) {
        super(message, cause);
    }
}
