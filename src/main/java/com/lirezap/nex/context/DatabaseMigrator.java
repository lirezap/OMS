package com.lirezap.nex.context;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.TRUE;

/**
 * Database migrator implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class DatabaseMigrator {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrator.class);

    private final Flyway flyway;

    DatabaseMigrator(final Configuration configuration) {
        var url = configuration.loadString("db.postgresql.url");
        var username = configuration.loadString("db.postgresql.username");
        var password = configuration.loadString("db.postgresql.password");

        flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("db/migration/postgresql")
                .executeInTransaction(TRUE)
                .load();
    }

    public void migrate() {
        flyway.migrate();
    }
}
