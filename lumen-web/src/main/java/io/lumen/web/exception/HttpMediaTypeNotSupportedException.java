package io.lumen.web.exception;

import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.HttpStatus;

@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class HttpMediaTypeNotSupportedException extends RuntimeException {
    public HttpMediaTypeNotSupportedException(String message) {
        super(message);
    }
}