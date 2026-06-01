package io.lumen.migration;

public interface DatabaseMigrator {

    void migrate();

    default void validate() {}

    default MigrationInfo info() {
        return null;
    }
}