package com.lirezap.nex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class to be executed.
 *
 * @author Alireza Pourtaghi
 */
public final class NexApplication {
    private static final Logger logger = LoggerFactory.getLogger(NexApplication.class);

    public static void main(final String[] args) {
        logger.info("Starting Nex; Version: {}", NexApplication.class.getPackage().getImplementationVersion());
    }
}
