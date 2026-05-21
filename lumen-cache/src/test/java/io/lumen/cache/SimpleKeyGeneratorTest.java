package io.lumen.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class SimpleKeyGeneratorTest {

    private SimpleKeyGenerator gen;

    @BeforeEach
    void setUp() {
        gen = new SimpleKeyGenerator();
    }

    // --- generate() ---

    @Test
    void generateNoArgs() throws Exception {
        Method m = Subject.class.getMethod("noArgs");
        assertEquals("noArgs", gen.generate(null, m));
    }

    @Test
    void generateOneArg() throws Exception {
        Method m = Subject.class.getMethod("oneArg", Long.class);
        assertEquals("oneArg:42", gen.generate(null, m, 42L));
    }

    @Test
    void generateMultipleArgs() throws Exception {
        Method m = Subject.class.getMethod("twoArgs", Long.class, String.class);
        String key = gen.generate(null, m, 1L, "hello").toString();
        assertTrue(key.startsWith("twoArgs:"));
        assertTrue(key.contains("1"));
        assertTrue(key.contains("hello"));
    }

    // --- resolveKey() ---

    @Test
    void resolveKeyBlankFallsBackToGenerate() throws Exception {
        Method m = Subject.class.getMethod("oneArg", Long.class);
        Object key = gen.resolveKey("", m, null, new Object[]{99L});
        assertEquals("oneArg:99", key);
    }

    @Test
    void resolveKeyParamExpression() throws Exception {
        Method m = Subject.class.getMethod("oneArg", Long.class);
        Object key = gen.resolveKey("#id", m, null, new Object[]{7L});
        assertEquals(7L, key);
    }

    @Test
    void resolveKeyLiteralPassedThrough() throws Exception {
        Method m = Subject.class.getMethod("oneArg", Long.class);
        Object key = gen.resolveKey("fixed-key", m, null, new Object[]{7L});
        assertEquals("fixed-key", key);
    }

    @Test
    void resolveKeyUnknownParamFallsBackToExpression() throws Exception {
        Method m = Subject.class.getMethod("oneArg", Long.class);
        // "#unknown" is not a param name — treated as literal
        Object key = gen.resolveKey("#unknown", m, null, new Object[]{7L});
        assertEquals("#unknown", key);
    }

    // helper class compiled with -parameters so param names are available
    @SuppressWarnings("unused")
    static class Subject {
        public void noArgs() {}
        public void oneArg(Long id) {}
        public void twoArgs(Long id, String name) {}
    }
}