package io.lumen.boot;

import io.lumen.core.context.Environment;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

public final class ApplicationPropertiesLoader {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationPropertiesLoader.class);
    private static final String PROFILES_PROPERTY = "lumen.profiles.active";

    private ApplicationPropertiesLoader() {}

    public static void load(Environment environment) {
        loadFile("application.properties", environment);

        String profilesValue = System.getProperty(PROFILES_PROPERTY);
        if (profilesValue == null) {
            profilesValue = environment.getProperty(PROFILES_PROPERTY);
        }

        if (profilesValue != null && !profilesValue.isBlank()) {
            String[] profiles = Arrays.stream(profilesValue.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            environment.setActiveProfiles(profiles);
            logger.info("Active profiles: {}", String.join(", ", profiles));
        }

        for (String profile : environment.getActiveProfiles()) {
            loadFile("application-" + profile + ".properties", environment);
        }
    }

    private static void loadFile(String filename, Environment environment) {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(filename)) {
            if (is == null) return;
            Properties props = new Properties();
            props.load(is);
            props.forEach((k, v) -> environment.setProperty((String) k, (String) v));
            logger.debug("Loaded {}", filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + filename, e);
        }
    }
}
