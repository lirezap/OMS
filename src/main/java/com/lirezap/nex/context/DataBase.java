package com.lirezap.nex.context;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database access class.
 *
 * @author Alireza Pourtaghi
 */
public final class DataBase {
    private static final Logger logger = LoggerFactory.getLogger(DataBase.class);

    private final DSLContext dslContext;

    DataBase(final Configuration configuration, final DataSource dataSource) {
        var settings = new Settings();
        settings.setQueryTimeout((int) configuration.loadDuration("db.postgresql.query_timeout").toSeconds());

        this.dslContext = DSL.using(dataSource.postgresql(), SQLDialect.POSTGRES, settings);
    }

    public DSLContext postgresql() {
        return dslContext;
    }
}
