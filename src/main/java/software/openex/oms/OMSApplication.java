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
package software.openex.oms;

import org.slf4j.Logger;

import static java.lang.System.exit;
import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.context.AppContext.context;
import static software.openex.oms.context.AppContext.initialize;

/**
 * Main application class to be executed.
 *
 * @author Alireza Pourtaghi
 */
public final class OMSApplication {
    private static final Logger logger = getLogger(OMSApplication.class);

    public static void main(final String... args) {
        try {
            initialize();
            context().databaseMigrator().migrate();
            context().matchingEngines().syncEventsWithDatabase();
            context().matchingEngines().loadOrderBooksFromDatabase();
            context().matchingEngines().start();
            context().socketServer().listen();

            logger.info("Started OMS; Version: {}", OMSApplication.class.getPackage().getImplementationVersion());
        } catch (Exception ex) {
            logger.error("error on initializing application context: {}", ex.getMessage(), ex);
            exit(-1);
        }
    }
}
