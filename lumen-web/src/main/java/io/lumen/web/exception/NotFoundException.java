package io.lumen.web.exception;

import io.lumen.web.annotation.ResponseStatus;
import io.lumen.web.http.HttpStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}