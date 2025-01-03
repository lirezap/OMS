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

import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.slf4j.Logger;

import static org.jooq.SQLDialect.POSTGRES;
import static org.jooq.impl.DSL.using;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Database access class.
 *
 * @author Alireza Pourtaghi
 */
public final class DataBase {
    private static final Logger logger = getLogger(DataBase.class);

    private final DSLContext dslContext;

    DataBase(final Configuration configuration, final DataSource dataSource) {
        final var settings = new Settings();
        settings.setQueryTimeout((int) configuration.loadDuration("db.postgresql.query_timeout").toSeconds());

        this.dslContext = using(dataSource.postgresql(), POSTGRES, settings);
    }

    public DSLContext postgresql() {
        return dslContext;
    }
}
