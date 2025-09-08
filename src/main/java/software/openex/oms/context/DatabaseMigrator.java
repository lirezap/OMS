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

        this.flyway = configure()
                .dataSource(url, username, password)
                .locations("db/migration/postgresql")
                .executeInTransaction(TRUE)
                .load();
    }

    public void migrate() {
        flyway.migrate();
    }
}
