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
