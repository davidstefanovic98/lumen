package io.lumen.flyway;

import io.lumen.core.context.Environment;
import io.lumen.migration.MigrationInfo;
import io.lumen.migration.NoOpDatabaseMigrator;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationTest {

    private DataSource dataSource;
    private Environment env;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:flyway_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        env = new Environment();
    }

    @Test
    void migrate_appliesScripts() {
        env.setProperty("lumen.flyway.locations", "classpath:db/test-migration");

        FlywayDatabaseMigrator migrator = new FlywayDatabaseMigrator(dataSource, env);
        migrator.migrate();

        MigrationInfo info = migrator.info();
        assertTrue(info.success());
    }

    @Test
    void migrate_schemaAlreadyUpToDate_isIdempotent() {
        env.setProperty("lumen.flyway.locations", "classpath:db/test-migration");

        FlywayDatabaseMigrator migrator = new FlywayDatabaseMigrator(dataSource, env);
        migrator.migrate();
        migrator.migrate();

        MigrationInfo info = migrator.info();
        assertTrue(info.success());
    }

    @Test
    void noOp_doesNothing() {
        NoOpDatabaseMigrator noop = new NoOpDatabaseMigrator();
        assertDoesNotThrow(noop::migrate);
        assertDoesNotThrow(noop::validate);

        MigrationInfo info = noop.info();
        assertEquals(0, info.migrationsExecuted());
        assertTrue(info.success());
    }
}