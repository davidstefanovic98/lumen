package io.lumen.web.exception;

import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.HttpStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class HttpMessageConvertException extends RuntimeException {
    public HttpMessageConvertException(String message) {
        super(message);
    }

    public HttpMessageConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}