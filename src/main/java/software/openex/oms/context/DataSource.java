/*
 * ISC License
 *
 * Copyright (c) 2025, Alireza Pourtaghi <lirezap@protonmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
