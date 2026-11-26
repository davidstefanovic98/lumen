package io.lumen.validation.web;

import io.lumen.validation.ConstraintViolation;
import io.lumen.validation.ValidationException;
import io.lumen.validation.Validator;
import io.lumen.validation.annotation.NotBlank;
import io.lumen.validation.annotation.Valid;
import io.lumen.web.exception.MethodArgumentNotValidException;
import io.lumen.web.http.ErrorResponse.FieldError;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ValidRequestBodyValidatorTest {

    record UserDto(@NotBlank String name) {}

    @SuppressWarnings("unused")
    static void validated(@Valid UserDto user) {}

    @SuppressWarnings("unused")
    static void unvalidated(UserDto user) {}

    private static Parameter param(String method) throws Exception {
        return ValidRequestBodyValidatorTest.class.getDeclaredMethod(method, UserDto.class).getParameters()[0];
    }

    private static final Validator FAILING = body -> List.of(
            new ConstraintViolation("name", "must not be blank"),
            new ConstraintViolation("name", "size must be between 2 and 50"));

    @Test
    void validParameterWithViolations_throwsMethodArgumentNotValid_carryingEveryViolation() throws Exception {
        ValidRequestBodyValidator bridge = new ValidRequestBodyValidator(() -> FAILING);

        MethodArgumentNotValidException ex = assertThrows(MethodArgumentNotValidException.class,
                () -> bridge.validate(new UserDto(""), param("validated")));

        assertEquals(List.of(new FieldError("name", "must not be blank"),
                new FieldError("name", "size must be between 2 and 50")), ex.getFieldErrors());
        assertEquals("Validation failed with 2 violation(s)", ex.getMessage());
    }

    @Test
    void originalValidationException_isKeptAsCause_soExceptionHandlersForItStillMatch() throws Exception {
        ValidRequestBodyValidator bridge = new ValidRequestBodyValidator(() -> FAILING);

        MethodArgumentNotValidException ex = assertThrows(MethodArgumentNotValidException.class,
                () -> bridge.validate(new UserDto(""), param("validated")));

        ValidationException cause = assertInstanceOf(ValidationException.class, ex.getCause());
        assertEquals(2, cause.getViolations().size());
    }

    @Test
    void parameterWithoutValid_isNotValidated() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ValidRequestBodyValidator bridge = new ValidRequestBodyValidator(() -> body -> {
            calls.incrementAndGet();
            return FAILING.validate(body);
        });

        assertDoesNotThrow(() -> bridge.validate(new UserDto(""), param("unvalidated")));
        assertEquals(0, calls.get());
    }

    @Test
    void validParameterWithoutViolations_passes() throws Exception {
        ValidRequestBodyValidator bridge = new ValidRequestBodyValidator(() -> body -> List.of());

        assertDoesNotThrow(() -> bridge.validate(new UserDto("Alice"), param("validated")));
    }
}
