package com.openex.oms.matching;

import com.openex.oms.binary.order.BuyOrder;
import com.openex.oms.binary.order.SellOrder;
import com.openex.oms.storage.ThreadSafeAtomicFile;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.openex.oms.context.AppContext.context;
import static java.nio.file.Path.of;
import static java.time.Duration.ofSeconds;
import static java.util.Comparator.reverseOrder;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A buy/sell orders matching engine.
 *
 * @author Alireza Pourtaghi
 */
public final class Engine implements Closeable {
    private static final Logger logger = getLogger(Engine.class);

    private final ExecutorService executor;
    private final ExecutorService importerExecutor;
    private final PriorityQueue<BuyOrder> buyOrders;
    private final PriorityQueue<SellOrder> sellOrders;
    private final ThreadSafeAtomicFile tradesFile;
    private final Matcher matcher;
    private final Importer importer;

    public Engine(final String symbol, final int initialCapacity) {
        this.executor = newSingleThreadExecutor();
        this.importerExecutor = newSingleThreadExecutor();
        this.buyOrders = new PriorityQueue<>(initialCapacity, reverseOrder());
        this.sellOrders = new PriorityQueue<>(initialCapacity, reverseOrder());
        this.tradesFile = tradesFile(symbol.strip().replace("/", ""));
        this.matcher = new Matcher(this.executor, this.buyOrders, this.sellOrders, this.tradesFile);
        this.importer = new Importer(this.importerExecutor, this.tradesFile);

        match();
        doImport();
    }

    private void match() {
        executor.submit(matcher);
    }

    private void doImport() {
        importerExecutor.submit(importer);
    }

    public CompletableFuture<Void> offer(final BuyOrder order) {
        final var future = new CompletableFuture<Void>();
        executor.submit(() -> {
            if (buyOrders.offer(order)) {
                future.complete(null);
                logger.trace("offer: buy: {}", order);
            } else {
                future.completeExceptionally(new RuntimeException("could not insert buy order into queue!"));
            }
        });

        return future;
    }

    public CompletableFuture<Void> offer(final SellOrder order) {
        final var future = new CompletableFuture<Void>();
        executor.submit(() -> {
            if (sellOrders.offer(order)) {
                future.complete(null);
                logger.trace("offer: sell: {}", order);
            } else {
                future.completeExceptionally(new RuntimeException("could not insert sell order into queue!"));
            }
        });

        return future;
    }

    private ThreadSafeAtomicFile tradesFile(final String symbol) {
        try {
            final var dataDirectoryPath = of(context().config().loadString("matching.engine.data_directory_path"));
            return new ThreadSafeAtomicFile(dataDirectoryPath.resolve(symbol + ".trades"), 1000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing matching engine ...");

        try {
            executor.shutdown();
            final var timeout = ofSeconds(60);
            if (!executor.awaitTermination(timeout.toSeconds(), SECONDS)) {
                // Safe to ignore runnable list!
                executor.shutdownNow();
            }

            importerExecutor.shutdown();
            if (!importerExecutor.awaitTermination(timeout.toSeconds(), SECONDS)) {
                // Safe to ignore runnable list!
                importerExecutor.shutdownNow();
            }

            tradesFile.close();
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        }
    }
}
