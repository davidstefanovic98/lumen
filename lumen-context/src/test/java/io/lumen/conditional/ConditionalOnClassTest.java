package io.lumen.conditional;

import io.lumen.context.annotation.*;
import io.lumen.context.AnnotationApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalOnClassTest {

    public static class TestConfig {

        @ConditionalOnClass(ProfileClassService.class)
        ExistingClassService existingService() {
            return new ExistingClassService();
        }

        @ConditionalOnClass(name = "com.fake.NonExistentClass")
        MissingClassService missingService() {
            return new MissingClassService();
        }

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
//        AnnotationApplicationContext context = new AnnotationApplicationContext(TestConfig.class);
//
//        TestConfig.ExistingClassService existing = context.getLight(TestConfig.ExistingClassService.class);
//        assertNotNull(existing);
//
//        assertThrows(RuntimeException.class, () -> context.getLight(TestConfig.MissingClassService.class));
    }
}
