package io.lumen.validation;

import java.util.List;

public class ValidationException extends RuntimeException {

    private final List<ConstraintViolation> violations;

    public ValidationException(List<ConstraintViolation> violations) {
        super("Validation failed with " + violations.size() + " violation(s)");
        this.violations = violations;
    }

    public List<ConstraintViolation> getViolations() {
        return violations;
    }
}