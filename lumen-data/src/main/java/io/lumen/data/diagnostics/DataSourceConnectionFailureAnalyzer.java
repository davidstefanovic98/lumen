package io.lumen.data.diagnostics;

import io.lumen.core.diagnostics.FailureAnalysis;
import io.lumen.core.diagnostics.StartupFailureAnalyzer;

import java.sql.SQLException;

/**
 * Recognizes a database connection failure. Hibernate eagerly opens a connection when
 * {@code LumenDataModule.configureJpa()} calls {@code buildSessionFactory()} (to detect the
 * dialect and, depending on {@code lumen.jpa.ddl-auto}, validate/create the schema), so a
 * misconfigured or unreachable database surfaces here rather than lazily on first repository use.
 *
 * <p>Matches on the JDK's own {@link SQLException} rather than a vendor-specific subtype
 * (e.g. PostgreSQL's {@code PSQLException}), so this doesn't need a compile-time dependency on
 * any particular JDBC driver.
 */
public class DataSourceConnectionFailureAnalyzer implements StartupFailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        if (!(failure instanceof SQLException sql)) {
            return null;
        }
        return new FailureAnalysis(
                "Failed to connect to the database: " + sql.getMessage(),
                "Check that the database is running and reachable, and that lumen.datasource.url, " +
                        "lumen.datasource.username and lumen.datasource.password in " +
                        "application.properties are correct."
        );
    }
}