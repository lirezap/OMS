package com.lirezap.nex.context;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC DataSource wrapper class to be used for database access or in jOOQ.
 *
 * @author Alireza Pourtaghi
 */
public final class DataSource {
    private static final Logger logger = LoggerFactory.getLogger(DataSource.class);

    private final HikariDataSource hikariDataSource;

    DataSource(final Configuration configuration) {
        var config = new HikariConfig();
        config.setJdbcUrl(configuration.loadString("db.postgresql.url"));
        config.setUsername(configuration.loadString("db.postgresql.username"));
        config.setPassword(configuration.loadString("db.postgresql.password"));
        config.setPoolName(configuration.loadString("db.postgresql.pool_name"));
        config.setMaximumPoolSize(configuration.loadInt("db.postgresql.max_pool_size"));
        config.setConnectionTimeout(configuration.loadDuration("db.postgresql.connection_timeout").toMillis());
        config.setKeepaliveTime(configuration.loadDuration("db.postgresql.keep_alive_time").toMillis());
        config.setMaxLifetime(configuration.loadDuration("db.postgresql.max_life_time").toMillis());

        this.hikariDataSource = new HikariDataSource(config);
        addShutdownHook();
    }

    public HikariDataSource postgresql() {
        return hikariDataSource;
    }

    /**
     * Adds a shutdown hook for current component.
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (!hikariDataSource.isClosed()) {
                    logger.info("Closing database connection pool ...");
                    hikariDataSource.close();
                }
            } catch (Exception ex) {
                logger.error("{}", ex.getMessage());
            }
        }));
    }
}
