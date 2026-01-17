package io.lumen.conditional;

import io.lumen.context.annotations.*;
import io.lumen.context.AnnotationApplicationContext;
import io.lumen.core.annotation.Value;
import io.lumen.core.component.ScopeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalContextTest {

    public static class TestConfig {

        @Light
        DevService devService() {
            return new DevService();
        }

        @Light
        @ConditionalOnProperty(name = "feature.enabled", havingValue = "true")
        FeatureService featureService() {
            return new FeatureService();
        }

        @Light
        @Scope(ScopeType.PROTOTYPE)
        MyService prototypeService(@Value("${my.property}") String property) {
            return new MyService(property);
        }

        static class DevService {}
        static class FeatureService {}
        static class MyService {
            private final String property;
            public MyService(String property) { this.property = property; }
            public String getProperty() { return property; }
        }
    }

    @Test
    void testConditionalRegistration() {
        AnnotationApplicationContext context = new AnnotationApplicationContext(TestConfig.class);

        // Dev service should always exist
        TestConfig.DevService dev = context.getLight(TestConfig.DevService.class);
        assertNotNull(dev);

        // Feature service should NOT exist because feature.enabled=false by default
        assertThrows(RuntimeException.class, () -> context.getLight(TestConfig.FeatureService.class));

        context.getEnvironment().setProperty("feature.enabled", "true");
        context.refresh();
        TestConfig.FeatureService feature = context.getLight(TestConfig.FeatureService.class);
        assertNotNull(feature);

        // Prototype should work as usual
        context.getEnvironment().setProperty("my.property", "Hello");
        context.refresh();
        TestConfig.MyService s1 = context.getLight(TestConfig.MyService.class);
        TestConfig.MyService s2 = context.getLight(TestConfig.MyService.class);
        assertNotSame(s1, s2);
        assertEquals("Hello", s1.getProperty());
        assertEquals("Hello", s2.getProperty());
    }
}