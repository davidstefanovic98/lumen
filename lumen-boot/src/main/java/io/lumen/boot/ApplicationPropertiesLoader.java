package io.lumen.boot;

import io.lumen.core.context.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ApplicationPropertiesLoader {

    private ApplicationPropertiesLoader() {}

    public static void load(Environment environment) {
        loadFile("application.properties", environment);
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + filename, e);
        }
    }
}
