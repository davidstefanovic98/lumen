package io.lumen.data;

import io.lumen.data.exception.InvalidDdlAutoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LumenDataModuleTest {

    private final LumenDataModule module = new LumenDataModule();

    @ParameterizedTest
    @ValueSource(strings = {"none", "validate", "update", "create", "create-drop"})
    void validDdlAutoValues_doNotThrow(String value) {
        assertDoesNotThrow(() -> module.validateDdlAuto(value));
    }

    @Test
    void unrecognizedDdlAutoValue_throwsWithClearMessage() {
        InvalidDdlAutoException ex = assertThrows(InvalidDdlAutoException.class,
                () -> module.validateDdlAuto("drop-and-create"));
        assertTrue(ex.getMessage().contains("drop-and-create"));
    }
}