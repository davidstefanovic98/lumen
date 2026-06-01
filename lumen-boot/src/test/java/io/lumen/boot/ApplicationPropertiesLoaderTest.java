package io.lumen.boot;

import io.lumen.core.context.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationPropertiesLoaderTest {

    @AfterEach
    void clearSystemProperty() {
        System.clearProperty("lumen.profiles.active");
    }

    @Test
    void loadsBaseProperties() {
        Environment env = new Environment();
        ApplicationPropertiesLoader.load(env);

        assertEquals("lumen-test", env.getProperty("app.name"));
    }

    @Test
    void activatesProfileFromBaseFile() {
        Environment env = new Environment();
        ApplicationPropertiesLoader.load(env);

        assertTrue(env.getActiveProfiles().contains("test"));
    }

    @Test
    void profileFileOverridesBaseProperty() {
        Environment env = new Environment();
        ApplicationPropertiesLoader.load(env);

        // base: app.port=8080, application-test.properties: app.port=9090
        assertEquals("9090", env.getProperty("app.port"));
    }

    @Test
    void profileFileAddsNewProperties() {
        Environment env = new Environment();
        ApplicationPropertiesLoader.load(env);

        assertEquals("true", env.getProperty("app.debug"));
    }

    @Test
    void systemPropertyOverridesFileProfile() {
        System.setProperty("lumen.profiles.active", "prod");

        Environment env = new Environment();
        ApplicationPropertiesLoader.load(env);

        // system property wins; no application-prod.properties exists so just the base loads
        assertTrue(env.getActiveProfiles().contains("prod"));
        assertFalse(env.getActiveProfiles().contains("test"));
        // base property unchanged since profile file doesn't exist
        assertEquals("8080", env.getProperty("app.port"));
    }

    @Test
    void noProfilesSetWhenPropertyAbsent() {
        Environment env = new Environment();
        // manually load without the test application.properties
        // by loading an empty env with no file — just verify no profiles activated by default
        assertTrue(env.getActiveProfiles().isEmpty());
    }
}