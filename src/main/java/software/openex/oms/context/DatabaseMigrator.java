package software.openex.oms.context;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;

import static java.lang.Boolean.TRUE;
import static org.flywaydb.core.Flyway.configure;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Database migrator implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class DatabaseMigrator {
    private static final Logger logger = getLogger(DatabaseMigrator.class);

    private final Flyway flyway;

    DatabaseMigrator(final Configuration configuration) {
        final var url = configuration.loadString("db.postgresql.url");
        final var username = configuration.loadString("db.postgresql.username");
        final var password = configuration.loadString("db.postgresql.password");

        flyway = configure()
                .dataSource(url, username, password)
                .locations("db/migration/postgresql")
                .executeInTransaction(TRUE)
                .load();
    }

    public void migrate() {
        flyway.migrate();
    }
}
