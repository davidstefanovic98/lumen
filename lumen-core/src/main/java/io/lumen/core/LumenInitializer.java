package io.lumen.core;

/**
 * Interface to be implemented in order to configure the ServletContext
 * programmatically as opposed to (or in addition to) annotation-based config.
 */
public interface LumenInitializer {
    void onStartup();
}
