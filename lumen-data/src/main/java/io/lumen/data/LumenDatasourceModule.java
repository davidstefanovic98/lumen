package io.lumen.data;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;

import javax.sql.DataSource;

@Order(5)
public class LumenDatasourceModule implements LumenModule {

    private static final Logger logger = LoggerFactory.getLogger(LumenDatasourceModule.class);

    @Override
    public void init(LightContainer container, String... basePackages) {
        Environment env = container.getLight(Environment.class);
        if (env == null || env.getProperty("lumen.datasource.url") == null) return;

        DataSource ds = tryBuildHikariDataSource(env);
        if (ds == null) {
            logger.debug("HikariCP not on classpath; DataSource bean not registered — lumen-data will use plain JDBC settings");
            return;
        }

        container.registerExternalInstance(DataSource.class, ds);
        logger.info("HikariCP DataSource registered (pool=LumenPool, url={})", env.getProperty("lumen.datasource.url"));
    }

    static DataSource tryBuildHikariDataSource(Environment env) {
        try {
            Class<?> cls = Class.forName("com.zaxxer.hikari.HikariDataSource");
            Object ds = cls.getDeclaredConstructor().newInstance();
            cls.getMethod("setJdbcUrl",           String.class).invoke(ds, env.getProperty("lumen.datasource.url"));
            cls.getMethod("setUsername",           String.class).invoke(ds, env.getProperty("lumen.datasource.username", ""));
            cls.getMethod("setPassword",           String.class).invoke(ds, env.getProperty("lumen.datasource.password", ""));
            cls.getMethod("setMaximumPoolSize",       int.class).invoke(ds, Integer.parseInt(env.getProperty("lumen.datasource.pool.max-size", "10")));
            cls.getMethod("setMinimumIdle",           int.class).invoke(ds, Integer.parseInt(env.getProperty("lumen.datasource.pool.min-idle", "2")));
            cls.getMethod("setConnectionTimeout",    long.class).invoke(ds, Long.parseLong(env.getProperty("lumen.datasource.pool.connection-timeout-ms", "30000")));
            cls.getMethod("setIdleTimeout",          long.class).invoke(ds, Long.parseLong(env.getProperty("lumen.datasource.pool.idle-timeout-ms", "600000")));
            cls.getMethod("setMaxLifetime",          long.class).invoke(ds, Long.parseLong(env.getProperty("lumen.datasource.pool.max-lifetime-ms", "1800000")));
            cls.getMethod("setPoolName",           String.class).invoke(ds, "LumenPool");
            return (DataSource) ds;
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure HikariCP", e);
        }
    }
}