package software.openex.oms.context;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * JDBC DataSource wrapper class to be used for database access or in jOOQ.
 *
 * @author Alireza Pourtaghi
 */
public final class DataSource implements Closeable {
    private static final Logger logger = getLogger(DataSource.class);

    private final HikariDataSource hikariDataSource;

    DataSource(final Configuration configuration) {
        final var config = new HikariConfig();
        config.setJdbcUrl(configuration.loadString("db.postgresql.url"));
        config.setUsername(configuration.loadString("db.postgresql.username"));
        config.setPassword(configuration.loadString("db.postgresql.password"));
        config.setPoolName(configuration.loadString("db.postgresql.pool_name"));
        config.setMaximumPoolSize(configuration.loadInt("db.postgresql.max_pool_size"));
        config.setConnectionTimeout(configuration.loadDuration("db.postgresql.connection_timeout").toMillis());
        config.setKeepaliveTime(configuration.loadDuration("db.postgresql.keep_alive_time").toMillis());
        config.setMaxLifetime(configuration.loadDuration("db.postgresql.max_life_time").toMillis());

        this.hikariDataSource = new HikariDataSource(config);
    }

    public HikariDataSource postgresql() {
        return hikariDataSource;
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing database connection pool ...");

        try {
            if (!hikariDataSource.isClosed()) {
                hikariDataSource.close();
            }
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        }
    }
}
