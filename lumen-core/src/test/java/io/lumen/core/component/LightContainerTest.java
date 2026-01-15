package io.lumen.core.component;

import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.DefaultApplicationContext;
import io.lumen.core.exception.CircularDependencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LightContainerTest {

    ApplicationContext context;
    @BeforeEach
    void setup() {
        context = new DefaultApplicationContext();
    }

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

    static class MyService {
        boolean initialized = false;
        public MyService() { initialized = true; }
    }

    static class MyLazyService {
        boolean initialized = false;
        public MyLazyService() { initialized = true; }
    }

    static class CollectionConsumer {
        final List<MyLazyService> services;
        public CollectionConsumer(List<MyLazyService> services) {
            this.services = services;
        }
    }

    @Test
    void testBasicRegistrationAndRetrieval() {
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
        Config config = new Config();

        context.registerInstance("config", config);
        context.initialize();

        Config retrieved = context.getLight(Config.class);
        assertSame(config, retrieved);
    }

    @Test
    void testRegisterFactory() {
        context.registerFactory("repoFactory", Repo.class, def -> new Repo());
        context.register(Service.class);

        context.initialize();

        Service service = context.getLight(Service.class);
        assertNotNull(service);
        assertNotNull(service.getRepo());
    }

    @Test
    void testCircularDependencyDetection() {
        context.register(CircularA.class);
        context.register(CircularB.class);

        Exception ex = assertThrows(CircularDependencyException.class, context::initialize);
        assertTrue(ex.getMessage().contains("Circular"));
    }

    @Test
    void testLazySingletonAndPrototype() {
        LightDefinition lazySingleton = LightDefinition.fromClass(MyLazyService.class, "lazyService");
        lazySingleton.setLazy(true);
        context.getLightContainer().registerDefinition(lazySingleton);

        LightDefinition prototype = LightDefinition.fromClass(MyService.class, "prototypeService");
        prototype.setScope(ScopeType.PROTOTYPE);
        context.getLightContainer().registerDefinition(prototype);

        LightDefinition consumerDef = LightDefinition.fromClass(CollectionConsumer.class);
        context.getLightContainer().registerDefinition(consumerDef);

        context.initialize();

        MyLazyService lazy = context.getLight("lazyService");
        assertTrue(lazy.initialized, "Lazy singleton should be initialized now");

        MyService proto1 = context.getLight(MyService.class);
        MyService proto2 = context.getLight(MyService.class);
        assertNotSame(proto1, proto2, "Prototype instances should be different");

        CollectionConsumer consumer = context.getLight(CollectionConsumer.class);
        assertFalse(consumer.services.isEmpty(), "Lazy collection should be created");
        for (MyLazyService s : consumer.services) {
            assertTrue(s.initialized, "Lazy service inside collection should be initialized on access");
        }
    }
}