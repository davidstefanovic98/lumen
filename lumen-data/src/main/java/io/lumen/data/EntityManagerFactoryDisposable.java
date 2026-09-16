package io.lumen.data;

import io.lumen.core.LumenDisposable;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import jakarta.persistence.EntityManagerFactory;

class EntityManagerFactoryDisposable implements LumenDisposable {

    private static final Logger logger = LoggerFactory.getLogger(EntityManagerFactoryDisposable.class);

    private final EntityManagerFactory entityManagerFactory;

    EntityManagerFactoryDisposable(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void onShutdown() {
        entityManagerFactory.close();
        logger.info("EntityManagerFactory closed.");
    }
}