package com.lirezap.nex.context;

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
