package io.lumen.validation;

import io.lumen.core.component.LightContainer;
import io.lumen.core.context.DefaultApplicationContext;
import io.lumen.validation.annotation.NotBlank;
import io.lumen.validation.annotation.Valid;
import io.lumen.validation.web.ValidRequestBodyValidator;
import io.lumen.web.argument.RequestBodyValidator;
import io.lumen.web.exception.MethodArgumentNotValidException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Parameter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LumenValidationModuleTest {

    record UserDto(@NotBlank String name) {}

    @SuppressWarnings("unused")
    static void handler(@Valid UserDto user) {}

    @Test
    void init_registersRequestBodyValidator_whereLumenWebLooksItUp() throws Exception {
        LightContainer container = new DefaultApplicationContext().getLightContainer();

        new LumenValidationModule().init(container);

        // The same lookup WebLumenInitializer hands to RequestBodyArgumentResolver
        List<RequestBodyValidator> validators = container.getLights(RequestBodyValidator.class);
        assertEquals(1, validators.size());
        assertInstanceOf(ValidRequestBodyValidator.class, validators.getFirst());

        // ...and it is backed by the container's Validator light (DefaultValidator here)
        Parameter param = LumenValidationModuleTest.class.getDeclaredMethod("handler", UserDto.class).getParameters()[0];
        MethodArgumentNotValidException ex = assertThrows(MethodArgumentNotValidException.class,
                () -> validators.getFirst().validate(new UserDto(" "), param));
        assertEquals("name", ex.getFieldErrors().getFirst().field());
    }
}
