package io.lumen.core.component;

import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.DefaultApplicationContext;
import io.lumen.core.exception.CircularDependencyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LightContainerTest {

    static class Config {}
    static class Repo {}
    static class Service {
        private final Repo repo;
        public Service(Repo repo) { this.repo = repo; }
        public Repo getRepo() { return repo; }
    }

    static class CircularA {
        public CircularA(CircularB b) {}
    }
    static class CircularB {
        public CircularB(CircularA a) {}
    }

    @Test
    void testBasicRegistrationAndRetrieval() {
        ApplicationContext context = new DefaultApplicationContext();

        context.register(Config.class);
        context.register(Repo.class);
        context.register(Service.class);

        context.initialize();

        Config config = context.getLight(Config.class);
        Repo repo = context.getLight(Repo.class);
        Service service = context.getLight(Service.class);

        assertNotNull(config);
        assertNotNull(repo);
        assertNotNull(service);
        assertSame(repo, service.getRepo());
    }

    @Test
    void testRegisterInstance() {
        ApplicationContext context = new DefaultApplicationContext();
        Config config = new Config();

        context.registerInstance("config", config);
        context.initialize();

        Config retrieved = context.getLight(Config.class);
        assertSame(config, retrieved);
    }

    @Test
    void testRegisterFactory() {
        ApplicationContext context = new DefaultApplicationContext();

        context.registerFactory("repoFactory", Repo.class, def -> new Repo());
        context.register(Service.class);

        context.initialize();

        Service service = context.getLight(Service.class);
        assertNotNull(service);
        assertNotNull(service.getRepo());
    }

    @Test
    void testCircularDependencyDetection() {
        ApplicationContext context = new DefaultApplicationContext();
        context.register(CircularA.class);
        context.register(CircularB.class);

        Exception ex = assertThrows(CircularDependencyException.class, context::initialize);
        assertTrue(ex.getMessage().contains("Circular"));
    }
}