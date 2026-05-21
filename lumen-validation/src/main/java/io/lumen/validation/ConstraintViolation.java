package io.lumen.validation;

public record ConstraintViolation(String field, String message) {}