package io.lumen.migration;

public record MigrationInfo(int migrationsExecuted, int migrationsSkipped, boolean success) {}