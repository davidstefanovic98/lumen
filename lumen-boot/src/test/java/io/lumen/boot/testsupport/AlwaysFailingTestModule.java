package io.lumen.boot.testsupport;

import io.lumen.core.LumenModule;
import io.lumen.core.component.LightContainer;

/**
 * Registered via {@code META-INF/services/io.lumen.core.LumenModule} in this module's test
 * resources, so it's picked up by every {@code ServiceLoader.load(LumenModule.class)} call made
 * while running lumen-boot's tests. It exists to reproduce, deterministically and without a real
 * database, the exact failure shape a module like {@code LumenFlywayModule} produces when its
 * {@code init()} throws (e.g. because the configured database is unreachable): {@link
 * io.lumen.boot.LumenApplicationTest} needs a {@code LumenModule} that fails during {@code new
 * AnnotationWebApplicationContext(configClass)} itself, before {@code startWebServer()} is ever
 * called, since that's precisely the phase whose failures used to be silently swallowed.
 *
 * <p>Always throws — there's currently no other test in this module that constructs a context,
 * so nothing else observes this. If that changes, that test will need to account for this module
 * always failing initialization.
 */
public class AlwaysFailingTestModule implements LumenModule {
    @Override
    public void init(LightContainer container, String... basePackages) {
        throw new RuntimeException("simulated module failure (AlwaysFailingTestModule)");
    }
}