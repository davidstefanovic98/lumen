package io.lumen.validation;

import java.util.List;

public interface Validator {

    List<ConstraintViolation> validate(Object object);

    default void validateAndThrow(Object object) {
        List<ConstraintViolation> violations = validate(object);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
    }
}