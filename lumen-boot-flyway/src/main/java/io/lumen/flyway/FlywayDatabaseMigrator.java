package io.lumen.flyway;

import io.lumen.core.context.Environment;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.migration.DatabaseMigrator;
import io.lumen.migration.MigrationInfo;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;

public class FlywayDatabaseMigrator implements DatabaseMigrator {

    private static final Logger logger = LoggerFactory.getLogger(FlywayDatabaseMigrator.class);

    private final Flyway flyway;
    private MigrationInfo lastResult;

    public FlywayDatabaseMigrator(DataSource dataSource, Environment env) {
        this.flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(env.getProperty("lumen.flyway.locations", "classpath:db/migration"))
                .baselineOnMigrate(Boolean.parseBoolean(env.getProperty("lumen.flyway.baseline-on-migrate", "false")))
                .table(env.getProperty("lumen.flyway.table", "flyway_schema_history"))
                .outOfOrder(Boolean.parseBoolean(env.getProperty("lumen.flyway.out-of-order", "false")))
                .load();
    }

    @Override
    public void migrate() {
        MigrateResult result = flyway.migrate();
        lastResult = new MigrationInfo(result.migrationsExecuted, 0, result.success);
        if (result.migrationsExecuted > 0) {
            logger.info("Flyway applied {} migration(s) (schema version: {})",
                    result.migrationsExecuted, result.targetSchemaVersion);
        } else {
            logger.info("Flyway: schema is up to date (version: {})", result.targetSchemaVersion);
        }
    }

    @Override
    public void validate() {
        flyway.validate();
    }

    @Override
    public MigrationInfo info() {
        return lastResult != null ? lastResult : new MigrationInfo(0, 0, true);
    }
}