package io.lumen.data;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class EntityManagerFactoryDisposableTest {

    private static EntityManagerFactory fakeEmf(AtomicBoolean closed) {
        return (EntityManagerFactory) Proxy.newProxyInstance(
                EntityManagerFactory.class.getClassLoader(),
                new Class[]{EntityManagerFactory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "close" -> { closed.set(true); yield null; }
                    case "isOpen" -> !closed.get();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @Test
    void onShutdown_closesEntityManagerFactory() {
        AtomicBoolean closed = new AtomicBoolean(false);
        EntityManagerFactory emf = fakeEmf(closed);

        new EntityManagerFactoryDisposable(emf).onShutdown();

        assertTrue(closed.get());
        assertFalse(emf.isOpen());
    }
}