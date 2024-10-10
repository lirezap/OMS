package com.lirezap.nex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lirezap.nex.context.AppContext.initialize;

/**
 * Main application class to be executed.
 *
 * @author Alireza Pourtaghi
 */
public final class NexApplication {
    private static final Logger logger = LoggerFactory.getLogger(NexApplication.class);

    public static void main(final String[] args) {
        logger.info("Starting Nex; Version: {}", NexApplication.class.getPackage().getImplementationVersion());

        try {
            initialize();
        } catch (Exception ex) {
            logger.error("Error on initializing application context: {}", ex.getMessage(), ex);
            System.exit(-1);
        }
    }
}
