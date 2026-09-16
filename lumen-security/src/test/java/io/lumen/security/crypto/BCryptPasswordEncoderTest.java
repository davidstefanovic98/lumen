package io.lumen.security.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderTest {

    @Test
    void defaultConstructor_usesValidLogRounds() {
        var encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("password");

        assertTrue(encoder.matches("password", encoded));
    }

    @Test
    void logRoundsBelowValidRange_throwsAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new BCryptPasswordEncoder(3));
    }

    @Test
    void logRoundsAboveValidRange_throwsAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new BCryptPasswordEncoder(32));
    }

    @Test
    void logRoundsAtValidRangeBoundaries_doesNotThrow() {
        assertDoesNotThrow(() -> new BCryptPasswordEncoder(4));
        assertDoesNotThrow(() -> new BCryptPasswordEncoder(31));
    }

    @Test
    void matches_wrongPassword_returnsFalse() {
        var encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("password");

        assertFalse(encoder.matches("wrong", encoded));
    }
}