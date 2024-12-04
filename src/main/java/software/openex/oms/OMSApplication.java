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
        logger.info("Starting OMS; Version: {}", OMSApplication.class.getPackage().getImplementationVersion());

        try {
            initialize();
            context().databaseMigrator().migrate();
            context().socketServer().listen();
        } catch (Exception ex) {
            logger.error("error on initializing application context: {}", ex.getMessage(), ex);
            exit(-1);
        }
    }
}
