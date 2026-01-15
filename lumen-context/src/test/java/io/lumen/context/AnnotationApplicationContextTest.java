package io.lumen.context;

import io.lumen.annotations.Light;
import io.lumen.annotations.Profile;
import io.lumen.annotations.Scope;
import io.lumen.annotations.Value;
import io.lumen.core.component.ScopeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationApplicationContextTest {

    public static class TestConfig {

        @Light
        public DevService devService() { return new DevService(); }

        @Light
        @Profile("prod")
        public ProdService prodService() { return new ProdService(); }

        @Light
        public ConditionalService conditionalService() { return new ConditionalService(); }

        @Light
        @Scope(ScopeType.PROTOTYPE)
        public MyService prototypeService(@Value("${my.property}") String property) {
            return new MyService(property);
        }

        // Dummy services
        static class DevService {}
        static class ProdService {}
        static class ConditionalService {}
        static class MyService {
            private final String property;
            public MyService(String property) { this.property = property; }
            public String getProperty() { return property; }
        }
    }

    @Test
    void testScanAndInjection() {
        AnnotationApplicationContext context = new AnnotationApplicationContext(TestConfig.class);
        context.getEnvironment().setProperty("my.property", "LumenTest");

        TestConfig.MyService p1 = context.getLight(TestConfig.MyService.class);
        TestConfig.MyService p2 = context.getLight(TestConfig.MyService.class);
        assertNotSame(p1, p2, "Prototype beans should be different instances");
        assertEquals("LumenTest", p1.getProperty());
        assertEquals("LumenTest", p2.getProperty());

        TestConfig.DevService dev = context.getLight(TestConfig.DevService.class);
        assertNotNull(dev);
    }
}
