package io.lumen.core.context;

/**
 * Interface to be implemented by lights that want to be aware of the
 * ApplicationContext (i.e., the DI container) that created them.
 * <p>
 * The container will call {@link #setApplicationContext(ApplicationContext)}
 * during initialization, before the light is used.
 */
public interface ApplicationContextAware {

    void setApplicationContext(ApplicationContext applicationContext);
}
