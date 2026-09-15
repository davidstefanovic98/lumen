package io.lumen.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Lumen framework's own version, sourced at build time from the parent pom's
 * {@code <version>} via resource filtering (see {@code lumen-version.properties} and
 * lumen-core's {@code pom.xml}) — never hand-maintained, so it cannot drift from a release.
 */
public final class LumenVersion {

    private static final String RESOURCE = "/io/lumen/core/lumen-version.properties";
    private static final String UNKNOWN = "unknown";
    private static final String VERSION = load();

    private LumenVersion() {}

    public static String get() {
        return VERSION;
    }

    private static String load() {
        try (InputStream in = LumenVersion.class.getResourceAsStream(RESOURCE)) {
            if (in == null) return UNKNOWN;
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version", UNKNOWN);
        } catch (IOException e) {
            return UNKNOWN;
        }
    }
}