package software.openex.oms.context;

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Executors list.
 *
 * @author Alireza Pourtaghi
 */
public final class Executors implements Closeable {
    private static final Logger logger = getLogger(Executors.class);

    private final ExecutorService workerExecutor;

    Executors(final Configuration configuration) {
        this.workerExecutor =
                configuration.loadBoolean("executors.worker.virtual_threads_enabled") ?
                        newVirtualThreadPerTaskExecutor() :
                        newFixedThreadPool(configuration.loadInt("executors.worker.threads"));
    }

    public ExecutorService worker() {
        return workerExecutor;
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing executors ...");

        try {
            workerExecutor.shutdown();
            final var timeout = ofSeconds(60);
            if (!workerExecutor.awaitTermination(timeout.toSeconds(), SECONDS)) {
                // Safe to ignore runnable list!
                workerExecutor.shutdownNow();
            }
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        }
    }
}
