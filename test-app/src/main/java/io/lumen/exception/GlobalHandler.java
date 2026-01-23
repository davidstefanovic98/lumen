package io.lumen.exception;

import io.lumen.web.annotation.ControllerAdvice;
import io.lumen.web.annotation.ExceptionHandler;
import io.lumen.web.exception.NotFoundException;
import io.lumen.web.http.ResponseEntity;

@ControllerAdvice
public class GlobalHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handle(Exception e) {
        return ResponseEntity.status(406).body("Custom Format Error: " + e.getMessage());
    }
}
