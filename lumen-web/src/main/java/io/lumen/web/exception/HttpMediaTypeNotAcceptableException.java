package io.lumen.web.exception;

import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.HttpStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class HttpMediaTypeNotAcceptableException extends RuntimeException {
    public HttpMediaTypeNotAcceptableException(String message) {
        super(message);
    }
}