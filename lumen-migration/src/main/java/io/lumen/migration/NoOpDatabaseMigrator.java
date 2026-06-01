package io.lumen.migration;

public class NoOpDatabaseMigrator implements DatabaseMigrator {

    @Override
    public void migrate() {}

    @Override
    public void validate() {}

    @Override
    public MigrationInfo info() {
        return new MigrationInfo(0, 0, true);
    }
}