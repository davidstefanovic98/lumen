package io.lumen.web.exception;

public class HttpMessageConvertException extends RuntimeException {
    public HttpMessageConvertException(String message) {
        super(message);
    }

    public HttpMessageConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}
