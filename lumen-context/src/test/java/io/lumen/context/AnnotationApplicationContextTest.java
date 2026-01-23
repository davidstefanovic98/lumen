package io.lumen.context;

import io.lumen.context.annotation.*;
import io.lumen.core.annotation.Value;
import io.lumen.core.component.ScopeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationApplicationContextTest {

    @ComponentScan(basePackages = "io.lumen.context")
    public static class TestConfig {

        @Profile("prod")
        public ProdService prodService() { return new ProdService(); }

        public ConditionalService conditionalService() { return new ConditionalService(); }

        @Scope(ScopeType.PROTOTYPE)
        public MyService prototypeService(@Value("${my.property}") String property) {
            return new MyService(property);
        }

        static class ProdService {}
        static class ConditionalService {}
        static class MyService {
            private final String property;
            public MyService(String property) { this.property = property; }
            public String getProperty() { return property; }
        }
    }

    @Service
    static class DevService {}

    @Test
    void testScanAndInjection() {
        AnnotationApplicationContext context = new AnnotationApplicationContext(TestConfig.class);
        context.getEnvironment().setProperty("my.property", "LumenTest");

        TestConfig.MyService p1 = context.getLight(TestConfig.MyService.class);
        TestConfig.MyService p2 = context.getLight(TestConfig.MyService.class);
        assertNotSame(p1, p2, "Prototype beans should be different instances");
        assertEquals("LumenTest", p1.getProperty());
        assertEquals("LumenTest", p2.getProperty());

        DevService dev = context.getLight(DevService.class);
        assertNotNull(dev);
    }

    @Test
    void testScanOfServices() {
        AnnotationApplicationContext context = new AnnotationApplicationContext(TestConfig.class);
        context.getEnvironment().setProperty("my.property", "LumenTest");

        TestConfig.MyService p1 = context.getLight(TestConfig.MyService.class);
        TestConfig.MyService p2 = context.getLight(TestConfig.MyService.class);
        assertNotSame(p1, p2, "Prototype beans should be different instances");
        assertEquals("LumenTest", p1.getProperty());
        assertEquals("LumenTest", p2.getProperty());

        DevService dev = context.getLight(DevService.class);
        assertNotNull(dev);
    }
}
