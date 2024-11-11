package com.lirezap.nex;

import org.slf4j.Logger;

import static com.lirezap.nex.context.AppContext.context;
import static com.lirezap.nex.context.AppContext.initialize;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Main application class to be executed.
 *
 * @author Alireza Pourtaghi
 */
public final class NexApplication {
    private static final Logger logger = getLogger(NexApplication.class);

    public static void main(final String... args) {
        logger.info("Starting Nex; Version: {}", NexApplication.class.getPackage().getImplementationVersion());

        try {
            initialize();
            context().databaseMigrator().migrate();
            context().nexServer().listen();
        } catch (Exception ex) {
            logger.error("error on initializing application context: {}", ex.getMessage(), ex);
            System.exit(-1);
        }
    }
}
