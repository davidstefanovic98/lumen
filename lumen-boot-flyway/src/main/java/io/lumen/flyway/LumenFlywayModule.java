package io.lumen.flyway;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.migration.DatabaseMigrator;
import io.lumen.migration.NoOpDatabaseMigrator;

import javax.sql.DataSource;

@Order(6)
public class LumenFlywayModule implements LumenModule {

    private static final Logger logger = LoggerFactory.getLogger(LumenFlywayModule.class);

    @Override
    public void init(LightContainer container, String... basePackages) {
        Environment env = container.getLight(Environment.class);
        if (env == null) return;

        boolean enabled = Boolean.parseBoolean(env.getProperty("lumen.flyway.enabled", "true"));
        if (!enabled) {
            logger.info("Flyway migration disabled (lumen.flyway.enabled=false)");
            container.registerExternalInstance(DatabaseMigrator.class, new NoOpDatabaseMigrator());
            return;
        }

        if (!container.hasLight(DataSource.class)) {
            logger.debug("No DataSource bean found; Flyway migration skipped");
            return;
        }

        DataSource ds = container.getLight(DataSource.class);
        FlywayDatabaseMigrator migrator = new FlywayDatabaseMigrator(ds, env);
        migrator.migrate();
        container.registerExternalInstance(DatabaseMigrator.class, migrator);
    }
}