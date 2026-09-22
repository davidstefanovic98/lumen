package io.lumen.data.transaction;

import io.lumen.data.annotation.Transactional;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test for the isolation-level leak: JpaTransactionManager.applyIsolation() used to
 * change a connection's isolation level via Connection.setTransactionIsolation() and never
 * restore it. A real pool (HikariCP) resets this on its own when the connection is returned, but
 * Hibernate's own built-in connection handling (no DataSource/pool configured) does not — this
 * test exercises exactly that path, using Hibernate's native bootstrap the same way
 * LumenDataModule.configureJpa() does when no DataSource light is registered.
 */
class IsolationRestoreTest {

    static EntityManagerFactory emf;
    static final String URL = "jdbc:h2:mem:isolation_restore_test;DB_CLOSE_DELAY=-1";

    @BeforeAll
    static void setup() {
        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .applySetting("hibernate.connection.pool_size", "1") // force reuse of one physical connection
                .applySetting("jakarta.persistence.jdbc.url", URL)
                .applySetting("jakarta.persistence.jdbc.user", "sa")
                .applySetting("jakarta.persistence.jdbc.password", "");
        StandardServiceRegistry serviceRegistry = builder.build();
        MetadataSources metadataSources = new MetadataSources(serviceRegistry);
        emf = metadataSources.buildMetadata().buildSessionFactory();
    }

    @AfterAll
    static void teardown() {
        if (emf != null) emf.close();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    static void serializableNoop() {}

    @Test
    void isolationLevel_isRestored_afterTransactionCompletes() throws Exception {
        JpaTransactionManager tm = new JpaTransactionManager(emf);

        int defaultLevel;
        try (Connection probe = DriverManager.getConnection(URL, "sa", "")) {
            defaultLevel = probe.getTransactionIsolation();
        }

        Transactional ann = IsolationRestoreTest.class.getDeclaredMethod("serializableNoop")
                .getAnnotation(Transactional.class);
        TransactionStatus status = tm.getTransaction(ann);
        tm.commit(status);

        // Next transaction reuses the same physical connection (pool_size=1) — it must see the
        // default isolation level, not the SERIALIZABLE level the previous transaction left.
        var em2 = emf.createEntityManager();
        var session = em2.unwrap(org.hibernate.Session.class);
        int[] levelAfter = new int[1];
        session.doWork(conn -> levelAfter[0] = conn.getTransactionIsolation());
        em2.close();

        assertEquals(defaultLevel, levelAfter[0],
                "isolation level must be restored once the transaction that changed it completes");
    }
}