package com.openex.oms.matching;

import com.openex.oms.binary.order.*;
import com.openex.oms.storage.ThreadSafeAtomicFile;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.openex.oms.context.AppContext.context;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.foreign.Arena.ofConfined;
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
    private final ExecutorService eventsSynchronizerExecutor;
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;
    private final ThreadSafeAtomicFile eventsFile;
    private final Matcher matcher;
    private final EventsSynchronizer eventsSynchronizer;

    public Engine(final String symbol, final int initialCapacity) {
        this.executor = newSingleThreadExecutor();
        this.eventsSynchronizerExecutor = newSingleThreadExecutor();
        this.buyOrders = new PriorityQueue<>(initialCapacity, reverseOrder());
        this.sellOrders = new PriorityQueue<>(initialCapacity, reverseOrder());
        this.eventsFile = eventsFile(symbol.strip().replace("/", ""));
        this.matcher = new Matcher(this.executor, this.buyOrders, this.sellOrders, this.eventsFile);
        this.eventsSynchronizer = new EventsSynchronizer(this.eventsSynchronizerExecutor, this.eventsFile);

        match();
        sync();
    }

    private void match() {
        executor.submit(matcher);
    }

    private void sync() {
        eventsSynchronizerExecutor.submit(eventsSynchronizer);
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

    public CompletableFuture<Boolean> cancel(final CancelOrder order) {
        final var future = new CompletableFuture<Boolean>();
        final var found = new AtomicBoolean(FALSE);

        executor.submit(() -> {
            buyOrders.forEach(buyOrder -> {
                if (buyOrder.equals(order)) {
                    found.set(TRUE);
                    buyOrders.remove(buyOrder);
                    buyOrderCanceled(future, order, buyOrder);
                }
            });

            if (!found.get()) {
                sellOrders.forEach(sellOrder -> {
                    if (sellOrder.equals(order)) {
                        found.set(TRUE);
                        sellOrders.remove(sellOrder);
                        sellOrderCanceled(future, order, sellOrder);
                    }
                });
            }

            if (!found.get()) {
                future.complete(FALSE);
            }
        });

        return future;
    }

    private void buyOrderCanceled(final CompletableFuture<Boolean> future, final CancelOrder order, final Order buyOrder) {
        try {
            append(order);
            future.complete(TRUE);
            logger.trace("cancel: buy: {}", order);
        } catch (RuntimeException ex) {
            // Re-offer the buy order at previous index.
            offer((BuyOrder) buyOrder);
            future.completeExceptionally(ex);
        }
    }

    private void sellOrderCanceled(final CompletableFuture<Boolean> future, final CancelOrder order, final Order sellOrder) {
        try {
            append(order);
            future.complete(TRUE);
            logger.trace("cancel: sell: {}", order);
        } catch (RuntimeException ex) {
            // Re-offer the sell order at previous index.
            offer((SellOrder) sellOrder);
            future.completeExceptionally(ex);
        }
    }

    private void append(final CancelOrder cancelOrder) {
        try (final var arena = ofConfined()) {
            final var binary = new OrderBinaryRepresentation(arena, cancelOrder);
            binary.encodeV1();

            eventsFile.append(binary.segment());
        }
    }

    private ThreadSafeAtomicFile eventsFile(final String symbol) {
        try {
            final var dataDirectoryPath = of(context().config().loadString("matching.engine.data_directory_path"));
            return new ThreadSafeAtomicFile(dataDirectoryPath.resolve(symbol + ".events"), 1000);
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

            eventsSynchronizerExecutor.shutdown();
            if (!eventsSynchronizerExecutor.awaitTermination(timeout.toSeconds(), SECONDS)) {
                // Safe to ignore runnable list!
                eventsSynchronizerExecutor.shutdownNow();
            }

            eventsFile.close();
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        }
    }
}
