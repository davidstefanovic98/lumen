package io.lumen.conditional;

import io.lumen.context.annotations.ConditionalOnLight;
import io.lumen.context.annotations.Light;
import io.lumen.context.annotations.Profile;
import io.lumen.context.AnnotationApplicationContext;
import io.lumen.core.exception.NoLightFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConditionalOnLightTest {

    public static class TestConfig {

        @Light
        @ConditionalOnLight(ProfileClassService.class)
        ExistingClassService existingService() {
            return new ExistingClassService();
        }

        @Light
        @Profile("dev")
        ProfileClassService profileService() {
            return new ProfileClassService();
        }

        static class ExistingClassService {}
        static class ProfileClassService {}
    }

    @Test
    void testConditionalOnLight() {
        AnnotationApplicationContext context = new AnnotationApplicationContext(TestConfig.class);
        assertThrows(NoLightFoundException.class, () -> context.getLight(TestConfig.ExistingClassService.class));
        assertThrows(NoLightFoundException.class, () -> context.getLight(TestConfig.ProfileClassService.class));
    }
}
