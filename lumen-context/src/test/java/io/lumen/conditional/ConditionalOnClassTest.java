package io.lumen.conditional;

import io.lumen.context.annotations.*;
import io.lumen.context.AnnotationApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalOnClassTest {

    public static class TestConfig {

        @Light
        @ConditionalOnClass(ProfileClassService.class)
        ExistingClassService existingService() {
            return new ExistingClassService();
        }

        @Light
        @ConditionalOnClass(name = "com.fake.NonExistentClass")
        MissingClassService missingService() {
            return new MissingClassService();
        }

        @Light
        @Profile("profile")
        ProfileClassService profileService() {
            return new ProfileClassService();
        }

        static class ExistingClassService {}
        static class MissingClassService {}
        static class ProfileClassService {}
    }

    @Test
    void testConditionalOnClass() {
        AnnotationApplicationContext context = new AnnotationApplicationContext(TestConfig.class);

        TestConfig.ExistingClassService existing = context.getLight(TestConfig.ExistingClassService.class);
        assertNotNull(existing);

        assertThrows(RuntimeException.class, () -> context.getLight(TestConfig.MissingClassService.class));
    }
}
