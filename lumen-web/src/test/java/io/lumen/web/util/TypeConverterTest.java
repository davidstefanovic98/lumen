package io.lumen.web.util;

import io.lumen.web.exception.TypeConversionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeConverterTest {

    @Test
    void convert_nullValue_returnsNull() {
        assertNull(TypeConverter.convert(null, Integer.class));
    }

    @Test
    void convert_validInt_succeeds() {
        assertEquals(42, TypeConverter.convert("42", int.class));
    }

    @Test
    void convert_validLong_succeeds() {
        assertEquals(42L, TypeConverter.convert("42", Long.class));
    }

    @Test
    void convert_validDouble_succeeds() {
        assertEquals(3.14, TypeConverter.convert("3.14", double.class));
    }

    @Test
    void convert_malformedInt_throwsTypeConversionExceptionWithCause() {
        var thrown = assertThrows(TypeConversionException.class,
                () -> TypeConverter.convert("not-a-number", int.class));

        assertInstanceOf(NumberFormatException.class, thrown.getCause());
        assertTrue(thrown.getMessage().contains("not-a-number"));
    }

    @Test
    void convert_malformedLong_throwsTypeConversionException() {
        assertThrows(TypeConversionException.class, () -> TypeConverter.convert("abc", long.class));
    }

    @Test
    void convert_malformedDouble_throwsTypeConversionException() {
        assertThrows(TypeConversionException.class, () -> TypeConverter.convert("abc", double.class));
    }

    @Test
    void convert_unsupportedType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> TypeConverter.convert("x", Object.class));
    }
}