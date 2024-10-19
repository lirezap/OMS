package com.lirezap.nex.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Executors list.
 *
 * @author Alireza Pourtaghi
 */
public final class Executors {
    private static final Logger logger = LoggerFactory.getLogger(Executors.class);

    private final ExecutorService workerExecutor;

    Executors(final Configuration configuration) {
        this.workerExecutor =
                configuration.loadBoolean("executors.worker.virtual_threads_enabled") ?
                        newVirtualThreadPerTaskExecutor() :
                        newFixedThreadPool(configuration.loadInt("executors.worker.threads"));

        addShutdownHook();
    }

    public ExecutorService worker() {
        return workerExecutor;
    }

    /**
     * Adds a shutdown hook for current component.
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("Closing executors ...");
                workerExecutor.shutdown();

                var timeout = Duration.ofSeconds(60);
                if (!workerExecutor.awaitTermination(timeout.toSeconds(), SECONDS)) {
                    // Safe to ignore runnable list!
                    workerExecutor.shutdownNow();
                }
            } catch (Exception ex) {
                logger.error("{}", ex.getMessage());
            }
        }));
    }
}
